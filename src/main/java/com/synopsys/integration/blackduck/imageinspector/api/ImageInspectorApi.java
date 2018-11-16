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
package com.synopsys.integration.blackduck.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.name.Names;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

@Component
public class ImageInspectorApi {
    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ImageInspector imageInspector;
    private Os os;
    private ComponentExtractorFactory componentExtractorFactory;
    private Gson gson;

    public ImageInspectorApi(Gson gson, ImageInspector imageInspector, ComponentExtractorFactory componentExtractorFactory, Os os) {
        this.gson = gson;
        this.imageInspector = imageInspector;
        this.componentExtractorFactory = componentExtractorFactory;
        this.os = os;
    }

    public SimpleBdioDocument getBdio(final String dockerTarfilePath, final String blackDuckProjectName, final String blackDuckProjectVersion,
            final String codeLocationPrefix, final String givenImageRepo, final String givenImageTag,
            final boolean cleanupWorkingDir,
            final String containerFileSystemOutputPath,
            final String currentLinuxDistro)
            throws IntegrationException {
        logger.info("getBdio()::");
        os.logMemory();
        final BdioGenerator bdioGenerator = new BdioGenerator(new SimpleBdioFactory());
        return getBdioDocument(bdioGenerator, dockerTarfilePath, blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, cleanupWorkingDir, containerFileSystemOutputPath,
                currentLinuxDistro);
    }

//    private String[] pkgListToBdio(final PackageManagerEnum pkgMgrType, String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName) throws IntegrationException {
//        logger.info("*** pkgListToBdio() String[] to String[]");
//        return new String[0];
//    }
//
//    private void pkgListToBdio(final PackageManagerEnum pkgMgrType, String linuxDistroName, final String pkgMgrListCmdOutputPath, final String bdioOutputPath, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName) throws IntegrationException {
//        logger.info("*** pkgListToBdio() file to file");
//        File pkgMgrListCmdOutputFile = new File(pkgMgrListCmdOutputPath);
//        String[] pkgMgrListCmdOutputLines = FileUtils.readLines(pkgMgrListCmdOutputFile, StandardCharsets.UTF_8).toArray();
//        String[] bdioLines = pkgListToBdio(pkgMgrType,  linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName);
//
//        File bdioOutputFile = new File(bdioOutputPath);
//        FileUtils.writeLines(bdioOutputFile, Arrays.asList(bdioLines));
//    }

    public void pkgListToBdioFile(final PackageManagerEnum pkgMgrType, String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String bdioOutputPath, final String blackDuckProjectName, final String blackDuckProjectVersion,
        final String codeLocationName)
        throws IntegrationException {
        logger.info(String.format("pkgListToBdioFile(): pkgMgrType: %s; pkgMgrListCmdOutput: %s, bdioOutputPath: %s; blackDuckProjectName: %s; blackDuckProjectVersion: %s; codeLocationName: %s",
            pkgMgrType, pkgMgrListCmdOutputLines, bdioOutputPath, blackDuckProjectName, blackDuckProjectVersion, codeLocationName));

        if (pkgMgrType != PackageManagerEnum.DPKG) {
            throw new UnsupportedOperationException("The pkgListToBdioFile() currently only supports DPKG");
        }
        ComponentExtractor extractor = (new ComponentExtractorFactory()).createComponentExtractor(gson, null, pkgMgrType);
        List<ComponentDetails> comps = extractor.extractComponentsFromPkgMgrOutput(linuxDistroName, pkgMgrListCmdOutputLines);
        logger.info(String.format("Extracted %d components from given package manager output", comps.size()));
        final BdioGenerator bdioGenerator = new BdioGenerator(new SimpleBdioFactory());
        SimpleBdioDocument bdioDoc = bdioGenerator.generateBdioDocument(codeLocationName, blackDuckProjectName, blackDuckProjectVersion, linuxDistroName, comps);
        File bdioOutputFile = new File(bdioOutputPath);
        try {
            imageInspector.writeBdioToFile(bdioDoc, bdioOutputFile);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error writing to BDIO output file %s", bdioOutputPath), e);
        }
    }

    private SimpleBdioDocument getBdioDocument(final BdioGenerator bdioGenerator, final String dockerTarfilePath, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationPrefix, final String givenImageRepo,
            final String givenImageTag,
            final boolean cleanupWorkingDir,
            final String containerFileSystemOutputPath, final String currentLinuxDistro)
            throws IntegrationException {
        final ImageInfoDerived imageInfoDerived = inspect(bdioGenerator, dockerTarfilePath, blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, cleanupWorkingDir,
                containerFileSystemOutputPath,
                currentLinuxDistro);
        return imageInfoDerived.getBdioDocument();
    }

    private ImageInfoDerived inspect(final BdioGenerator bdioGenerator, final String dockerTarfilePath, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationPrefix, final String givenImageRepo, final String givenImageTag,
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
            imageInfoDerived = inspectUsingGivenWorkingDir(bdioGenerator, dockerTarfile, blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, containerFileSystemOutputPath, currentLinuxDistro,
                    tempDir, cleanupWorkingDir);
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

    private ImageInfoDerived inspectUsingGivenWorkingDir(final BdioGenerator bdioGenerator, final File dockerTarfile, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationPrefix, final String givenImageRepo,
            final String givenImageTag,
            final String containerFileSystemOutputPath,
            final String currentLinuxDistro, final File tempDir, final boolean cleanupWorkingDir)
            throws IOException, IntegrationException, WrongInspectorOsException, InterruptedException, CompressorException {
        final File workingDir = new File(tempDir, "working");
        logger.debug(String.format("imageInspector: %s; workingDir: %s", imageInspector, workingDir.getAbsolutePath()));
        final List<File> layerTars = imageInspector.extractLayerTars(workingDir, dockerTarfile);
        final ManifestLayerMapping imageMetadata = imageInspector.getLayerMapping(workingDir, dockerTarfile.getName(), givenImageRepo, givenImageTag);
        final String imageRepo = imageMetadata.getImageName();
        final String imageTag = imageMetadata.getTagName();
        final File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        final OperatingSystemEnum currentOs = os.deriveOs(currentLinuxDistro);
        imageInspector.extractDockerLayers(currentOs, targetImageFileSystemRootDir, layerTars, imageMetadata);
        cleanUpLayerTars(cleanupWorkingDir, layerTars);
        OperatingSystemEnum inspectorOs = null;
        ImageInfoDerived imageInfoDerived;
        try {
            final ImageInfoParsed imageInfoParsed = imageInspector.parseImageInfo(targetImageFileSystemRootDir);
            inspectorOs = imageInfoParsed.getPkgMgr().getPackageManager().getInspectorOperatingSystem();
            if (!inspectorOs.equals(currentOs)) {
                final ImageInspectorOsEnum neededInspectorOs = ImageInspectorOsEnum.getImageInspectorOsEnum(inspectorOs);
                final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs);
                throw new WrongInspectorOsException(neededInspectorOs, msg);
            }
            imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(bdioGenerator, imageInfoParsed, imageRepo, imageTag, imageMetadata, blackDuckProjectName, blackDuckProjectVersion, targetImageFileSystemRootDir,
                    codeLocationPrefix);
        } catch (final PkgMgrDataNotFoundException e) {
            imageInfoDerived = imageInspector.generateEmptyBdio(bdioGenerator, imageRepo, imageTag, imageMetadata, blackDuckProjectName, blackDuckProjectVersion, targetImageFileSystemRootDir,
                    codeLocationPrefix);
        }
        createContainerFileSystemTarIfRequested(targetImageFileSystemRootDir, containerFileSystemOutputPath);
        return imageInfoDerived;
    }

    private void cleanUpLayerTars(final boolean cleanupWorkingDir, final List<File> layerTars) {
        if (cleanupWorkingDir) {
            for (final File layerTar : layerTars) {
                logger.trace(String.format("Deleting %s", layerTar.getAbsolutePath()));
                FileUtils.deleteQuietly(layerTar);
            }
        }
    }

    private void createContainerFileSystemTarIfRequested(final File targetImageFileSystemRootDir, final String containerFileSystemOutputPath) throws IOException, CompressorException {
        if (StringUtils.isNotBlank(containerFileSystemOutputPath)) {
            logger.info("Including container file system in output");
            final File outputDirectory = new File(containerFileSystemOutputPath);
            final File containerFileSystemTarFile = new File(containerFileSystemOutputPath);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final LinuxFileSystem containerFileSys = new LinuxFileSystem(targetImageFileSystemRootDir);
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

}
