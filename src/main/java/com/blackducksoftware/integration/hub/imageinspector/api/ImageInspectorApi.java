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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInfoDerived;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInspector;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.linux.Os;

@Component
public class ImageInspectorApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ImageInspector imageInspector;

    @Autowired
    private Os os;

    public SimpleBdioDocument getBdio(final String dockerTarfilePath, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix, final boolean cleanupWorkingDir)
            throws IOException, HubIntegrationException, InterruptedException {
        logger.info("getBdio()");
        return getBdioDocument(dockerTarfilePath, hubProjectName, hubProjectVersion, codeLocationPrefix, cleanupWorkingDir);
    }

    private SimpleBdioDocument getBdioDocument(final String dockerTarfilePath, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix, final boolean cleanupWorkingDir)
            throws IOException, HubIntegrationException, InterruptedException {
        final ImageInfoDerived imageInfoDerived = inspect(dockerTarfilePath, hubProjectName, hubProjectVersion, codeLocationPrefix, cleanupWorkingDir);
        return imageInfoDerived.getBdioDocument();
    }

    ImageInfoDerived inspect(final String dockerTarfilePath, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix, final boolean cleanupWorkingDir)
            throws IOException, HubIntegrationException, InterruptedException {
        final File dockerTarfile = new File(dockerTarfilePath);
        final File tempDir = createTempDirectory();
        ImageInfoDerived imageInfoDerived = null;
        try {
            imageInfoDerived = inspectUsingGivenWorkingDir(dockerTarfile, hubProjectName, hubProjectVersion, codeLocationPrefix, tempDir);
        } finally {
            if (cleanupWorkingDir) {
                logger.info(String.format("Deleting working dir %s", tempDir.getAbsolutePath()));
                deleteDir(tempDir);
            }
        }
        return imageInfoDerived;
    }

    // TODO move this
    private void deleteDir(final File tempDir) {
        for (int i = 0; i < 10; i++) {
            logger.debug(String.format("Attempt $%d to delete dir %s", i, tempDir.getAbsolutePath()));
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (final IOException e) {
                logger.warn(String.format("Error deleting dir %s: %s", tempDir.getAbsolutePath(), e.getMessage()));
            }
            if (!tempDir.exists()) {
                logger.debug(String.format("Dir %s has been deleted", tempDir.getAbsolutePath()));
                return;
            }
            try {
                Thread.sleep(1000L);
            } catch (final InterruptedException e) {
                logger.warn(String.format("deleteDir() sleep interrupted: %s", e.getMessage()));
            }
        }
        logger.warn(String.format("Unable to delete dir %s", tempDir.getAbsolutePath()));
    }

    private ImageInfoDerived inspectUsingGivenWorkingDir(final File dockerTarfile, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix, final File tempDir)
            throws IOException, HubIntegrationException, WrongInspectorOsException, InterruptedException {
        final File workingDir = new File(tempDir, "working");
        logger.debug(String.format("imageInspector: %s", imageInspector));
        final List<File> layerTars = imageInspector.extractLayerTars(workingDir, dockerTarfile);
        final List<ManifestLayerMapping> tarfileMetadata = imageInspector.getLayerMappings(workingDir, dockerTarfile.getName(), null, null);
        if (tarfileMetadata.size() != 1) {
            final String msg = String.format("Expected a single image tarfile, but %s has %d images", dockerTarfile.getAbsolutePath(), tarfileMetadata.size());
            throw new HubIntegrationException(msg);
        }
        final ManifestLayerMapping imageMetadata = tarfileMetadata.get(0);
        final String imageRepo = imageMetadata.getImageName();
        final String imageTag = imageMetadata.getTagName();
        /// end parse manifest
        final File targetImageFileSystemRootDir = imageInspector.extractDockerLayers(workingDir, imageRepo, imageTag, layerTars, tarfileMetadata);
        final OperatingSystemEnum currentOs = os.deriveCurrentOs();
        final OperatingSystemEnum targetOs = imageInspector.detectOperatingSystem(targetImageFileSystemRootDir);
        if (!targetOs.equals(currentOs)) {
            final ImageInspectorOsEnum neededInspectorOs = getImageInspectorOsEnum(targetOs);
            final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs);
            throw new WrongInspectorOsException(dockerTarfile.getAbsolutePath(), neededInspectorOs, msg);
        }
        final ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(imageRepo, imageTag, tarfileMetadata, hubProjectName, hubProjectVersion, dockerTarfile, targetImageFileSystemRootDir, targetOs,
                codeLocationPrefix);
        return imageInfoDerived;
    }

    private File createTempDirectory() throws IOException {
        final String suffix = String.format("_%s_%s", Thread.currentThread().getName(), Long.toString((new Date()).getTime()));
        final File temp = File.createTempFile("ImageInspectorApi_", suffix);
        logger.info(String.format("Creating working dir %s", temp.getAbsolutePath()));
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }

    private ImageInspectorOsEnum getImageInspectorOsEnum(final OperatingSystemEnum osEnum) throws HubIntegrationException {
        switch (osEnum) {
        case UBUNTU:
            return ImageInspectorOsEnum.UBUNTU;
        case CENTOS:
            return ImageInspectorOsEnum.CENTOS;
        case ALPINE:
            return ImageInspectorOsEnum.ALPINE;
        default:
            throw new HubIntegrationException("");
        }
    }
}
