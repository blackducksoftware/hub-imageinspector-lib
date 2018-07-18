/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInfoDerived;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInspector;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.linux.FileOperations;
import com.blackducksoftware.integration.hub.imageinspector.linux.FileSys;
import com.blackducksoftware.integration.hub.imageinspector.linux.Os;

@Component
public class ImageInspectorApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ImageInspector imageInspector;

    @Autowired
    private Os os;

    public SimpleBdioDocument getBdio(final String dockerTarfilePath, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix, final String givenImageRepo, final String givenImageTag,
            final boolean cleanupWorkingDir, final String containerFileSystemOutputPath,
            final String currentLinuxDistro)
            throws IntegrationException {
        logger.info("getBdio()");
        os.logMemory();
        return getBdioDocument(dockerTarfilePath, hubProjectName, hubProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, cleanupWorkingDir, containerFileSystemOutputPath, currentLinuxDistro);
    }

    private SimpleBdioDocument getBdioDocument(final String dockerTarfilePath, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix, final String givenImageRepo, final String givenImageTag,
            final boolean cleanupWorkingDir,
            final String containerFileSystemOutputPath, final String currentLinuxDistro)
            throws IntegrationException {
        final ImageInfoDerived imageInfoDerived = inspect(dockerTarfilePath, hubProjectName, hubProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, cleanupWorkingDir, containerFileSystemOutputPath, currentLinuxDistro);
        return imageInfoDerived.getBdioDocument();
    }

    ImageInfoDerived inspect(final String dockerTarfilePath, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix, final String givenImageRepo, final String givenImageTag,
            final boolean cleanupWorkingDir, final String containerFileSystemOutputPath,
            final String currentLinuxDistro)
            throws IntegrationException {
        final File dockerTarfile = new File(dockerTarfilePath);
        File tempDir;
        try {
            tempDir = createTempDirectory();
        } catch (final IOException e) {
            throw new IntegrationException(String.format("Error creating temp dir: %s", e.getMessage()), e);
        }
        ImageInfoDerived imageInfoDerived = null;
        try {
            imageInfoDerived = inspectUsingGivenWorkingDir(dockerTarfile, hubProjectName, hubProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, containerFileSystemOutputPath, currentLinuxDistro, tempDir);
        } catch (IOException | InterruptedException | CompressorException e) {
            throw new IntegrationException(String.format("Error inspecting image: %s", e.getMessage()), e);
        } finally {
            if (cleanupWorkingDir) {
                logger.info(String.format("Deleting working dir %s", tempDir.getAbsolutePath()));
                FileOperations.deleteDirPersistently(tempDir);
            }
        }
        return imageInfoDerived;
    }

    private ImageInfoDerived inspectUsingGivenWorkingDir(final File dockerTarfile, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix, final String givenImageRepo, final String givenImageTag,
            final String containerFileSystemOutputPath,
            final String currentLinuxDistro, final File tempDir)
            throws IOException, IntegrationException, WrongInspectorOsException, InterruptedException, CompressorException {
        final File workingDir = new File(tempDir, "working");
        logger.debug(String.format("imageInspector: %s", imageInspector));
        final List<File> layerTars = imageInspector.extractLayerTars(workingDir, dockerTarfile);
        final List<ManifestLayerMapping> tarfileMetadata = imageInspector.getLayerMappings(workingDir, dockerTarfile.getName(), givenImageRepo, givenImageTag);
        if (tarfileMetadata.size() != 1) {
            final String msg = String.format("Expected a single image tarfile, but %s has %d images", dockerTarfile.getAbsolutePath(), tarfileMetadata.size());
            throw new IntegrationException(msg);
        }
        final ManifestLayerMapping imageMetadata = tarfileMetadata.get(0);
        final String imageRepo = imageMetadata.getImageName();
        final String imageTag = imageMetadata.getTagName();
        /// end parse manifest
        final File targetImageFileSystemRootDir = imageInspector.extractDockerLayers(workingDir, imageRepo, imageTag, layerTars, tarfileMetadata);
        final OperatingSystemEnum currentOs = os.deriveCurrentOs(currentLinuxDistro);
        OperatingSystemEnum inspectorOs = null;
        ImageInfoDerived imageInfoDerived;
        try {
            inspectorOs = imageInspector.detectInspectorOperatingSystem(targetImageFileSystemRootDir);
            if (!inspectorOs.equals(currentOs)) {
                final ImageInspectorOsEnum neededInspectorOs = getImageInspectorOsEnum(inspectorOs);
                final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs);
                throw new WrongInspectorOsException(dockerTarfile.getAbsolutePath(), neededInspectorOs, msg);
            }
            imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(imageRepo, imageTag, tarfileMetadata, hubProjectName, hubProjectVersion, dockerTarfile, targetImageFileSystemRootDir, inspectorOs,
                    codeLocationPrefix);
        } catch (final PkgMgrDataNotFoundException e) {
            imageInfoDerived = imageInspector.generateEmptyBdio(imageRepo, imageTag, tarfileMetadata, hubProjectName, hubProjectVersion, dockerTarfile, targetImageFileSystemRootDir, inspectorOs,
                    codeLocationPrefix);
        }
        createContainerFileSystemTarIfRequested(targetImageFileSystemRootDir, containerFileSystemOutputPath);
        return imageInfoDerived;
    }

    private void createContainerFileSystemTarIfRequested(final File targetImageFileSystemRootDir, final String containerFileSystemOutputPath) throws IOException, CompressorException {
        if (StringUtils.isNotBlank(containerFileSystemOutputPath)) {
            logger.info("Including container file system in output");
            final File outputDirectory = new File(containerFileSystemOutputPath);
            final File containerFileSystemTarFile = new File(containerFileSystemOutputPath);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final FileSys containerFileSys = new FileSys(targetImageFileSystemRootDir);
            containerFileSys.createTarGz(containerFileSystemTarFile);
        }
    }

    private File createTempDirectory() throws IOException {
        final String suffix = String.format("_%s_%s", Thread.currentThread().getName(), Long.toString(new Date().getTime()));
        final File temp = File.createTempFile("ImageInspectorApi_", suffix);
        logger.info(String.format("Creating working dir %s", temp.getAbsolutePath()));
        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        FileOperations.logFreeDiskSpace(temp);
        return temp;
    }

    private ImageInspectorOsEnum getImageInspectorOsEnum(final OperatingSystemEnum osEnum) throws IntegrationException {
        switch (osEnum) {
        case UBUNTU:
            return ImageInspectorOsEnum.UBUNTU;
        case CENTOS:
            return ImageInspectorOsEnum.CENTOS;
        case ALPINE:
            return ImageInspectorOsEnum.ALPINE;
        default:
            throw new IntegrationException("");
        }
    }

}
