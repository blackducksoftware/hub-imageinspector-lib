/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.Manifest;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class DockerTarParser {
    static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    private static final String DOCKER_LAYER_TAR_FILENAME = "layer.tar";
    private static final String DOCKER_LAYER_METADATA_FILENAME = "json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Gson gson = new Gson();

    private ManifestFactory manifestFactory;
    private Os os;

    @Autowired
    public void setOs(final Os os) {
        this.os = os;
    }

    @Autowired
    public void setManifestFactory(final ManifestFactory manifestFactory) {
        this.manifestFactory = manifestFactory;
    }

    public void extractDockerLayers(final ComponentExtractorFactory componentExtractorFactory, final OperatingSystemEnum currentOs, final File targetImageFileSystemRootDir, final List<File> layerTars, final ManifestLayerMapping manifestLayerMapping) throws WrongInspectorOsException, IOException {
        for (final String layer : manifestLayerMapping.getLayers()) {
            logger.trace(String.format("Looking for tar for layer: %s", layer));
            final File layerTar = getLayerTar(layerTars, layer);
            if (layerTar != null) {
                extractLayerTarToDir(targetImageFileSystemRootDir, layerTar);
                logLayerMetadata(layerTar);
                logComponentsPresentAfterAddingThisLayer(componentExtractorFactory, currentOs,  targetImageFileSystemRootDir);
            } else {
                logger.error(String.format("Could not find the tar for layer %s", layer));
            }
        }
    }

    public ImageInfoParsed parseImageInfo(final File targetImageFileSystemRootDir) throws PkgMgrDataNotFoundException {
        logger.debug(String.format("Checking image file system at %s for package managers", targetImageFileSystemRootDir.getName()));
        for (final PackageManagerEnum packageManagerEnum : PackageManagerEnum.values()) {
            if (packageManagerEnum == PackageManagerEnum.NULL) {
                continue;
            }
            final File packageManagerDirectory = new File(targetImageFileSystemRootDir, packageManagerEnum.getDirectory());
            if (packageManagerDirectory.exists()) {
                logger.info(String.format("Found package Manager Dir: %s", packageManagerDirectory.getAbsolutePath()));
                final ImagePkgMgrDatabase targetImagePkgMgr = new ImagePkgMgrDatabase(packageManagerDirectory, packageManagerEnum);
                final String linuxDistroName = extractLinuxDistroNameFromFileSystem(targetImageFileSystemRootDir).orElse(null);
                final ImageInfoParsed imagePkgMgrInfo = new ImageInfoParsed(targetImageFileSystemRootDir, targetImagePkgMgr, linuxDistroName);
                return imagePkgMgrInfo;
            } else {
                logger.debug(String.format("Package manager dir %s does not exist", packageManagerDirectory.getAbsolutePath()));
            }
        }
        throw new PkgMgrDataNotFoundException("No package manager files found in this Docker image.");
    }

    public List<File> extractLayerTars(final File workingDirectory, final File dockerTar) throws IOException {
        logger.debug(String.format("working dir: %s", workingDirectory));
        final File tarExtractionDirectory = getTarExtractionDirectory(workingDirectory);
        final List<File> untaredFiles = new ArrayList<>();
        final File outputDir = new File(tarExtractionDirectory, dockerTar.getName());
        final TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(dockerTar));
        try {
            TarArchiveEntry tarArchiveEntry = null;
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                final File outputFile = new File(outputDir, tarArchiveEntry.getName());
                if (tarArchiveEntry.isFile()) {
                    if (!outputFile.getParentFile().exists()) {
                        outputFile.getParentFile().mkdirs();
                    }
                    final OutputStream outputFileStream = new FileOutputStream(outputFile);
                    try {
                        logger.trace(String.format("Untarring %s", outputFile.getAbsolutePath()));
                        IOUtils.copy(tarArchiveInputStream, outputFileStream);
                        if (tarArchiveEntry.getName().contains(DOCKER_LAYER_TAR_FILENAME)) {
                            untaredFiles.add(outputFile);
                        }
                    } finally {
                        outputFileStream.close();
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream);
        }
        return untaredFiles;
    }

    public ManifestLayerMapping getLayerMapping(final File workingDirectory, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        logger.debug(String.format("getLayerMappings(): dockerImageName: %s; dockerTagName: %s", dockerImageName, dockerTagName));
        logger.debug(String.format("working dir: %s", workingDirectory));
        final Manifest manifest = manifestFactory.createManifest(getTarExtractionDirectory(workingDirectory), tarFileName);
        ManifestLayerMapping mapping;
        try {
            mapping = manifest.getLayerMapping(dockerImageName, dockerTagName);
        } catch (final Exception e) {
            final String msg = String.format("Could not parse the image manifest file : %s", e.getMessage());
            logger.error(msg);
            throw new IntegrationException(msg, e);
        }
        return mapping;
    }

    public File getTarExtractionDirectory(final File workingDirectory) {
        return new File(workingDirectory, TAR_EXTRACTION_DIRECTORY);
    }

    private Optional<String> extractLinuxDistroNameFromFileSystem(final File targetImageFileSystemRootDir) {
        final LinuxFileSystem extractedFileSys = new LinuxFileSystem(targetImageFileSystemRootDir);
        final Optional<File> etcDir = extractedFileSys.getEtcDir();
        if (!etcDir.isPresent()) {
            return Optional.empty();
        }
        return extractLinuxDistroNameFromEtcDir(etcDir.get());
    }

    private Optional<String> extractLinuxDistroNameFromEtcDir(final File etcDir) {
        logger.trace(String.format("/etc directory: %s", etcDir.getAbsolutePath()));
        if (etcDir.listFiles().length == 0) {
            logger.warn(String.format("Could not determine the Operating System because the /etc dir (%s) is empty", etcDir.getAbsolutePath()));
        }
        return extractLinuxDistroNameFromFiles(etcDir.listFiles());
    }

    private Optional<String> extractLinuxDistroNameFromFiles(final File[] etcFiles) {
        for (final File etcFile : etcFiles) {
            if (os.isLinuxDistroFile(etcFile)) {
                return os.getLinxDistroName(etcFile);
            }
        }
        return Optional.empty();
    }

    private File extractLayerTarToDir(final File targetImageFileSystemRoot, final File layerTar) throws IOException {
        logger.trace(String.format("Extracting layer: %s into %s", layerTar.getAbsolutePath(), targetImageFileSystemRoot.getAbsolutePath()));
        final DockerLayerTar dockerLayerTar = new DockerLayerTar(layerTar);
        final List<File> filesToRemove = dockerLayerTar.extractToDir(targetImageFileSystemRoot);
        for (final File fileToRemove : filesToRemove) {
            if (fileToRemove.isDirectory()) {
                logger.debug(String.format("Removing dir marked for deletion: %s", fileToRemove.getAbsolutePath()));
                FileUtils.deleteDirectory(fileToRemove);
            } else {
                logger.debug(String.format("Removing file marked for deletion: %s", fileToRemove.getAbsolutePath()));
                FileUtils.deleteQuietly(fileToRemove);
            }
        }
        return targetImageFileSystemRoot;
    }

    private File getLayerTar(final List<File> layerTars, final String layer) {
        File layerTar = null;
        for (final File candidateLayerTar : layerTars) {
            if (layer.equals(candidateLayerTar.getParentFile().getName())) {
                logger.debug(String.format("Found layer tar for layer %s", layer));
                layerTar = candidateLayerTar;
                break;
            }
        }
        return layerTar;
    }

    private void logLayerMetadata(final File layerTarFile) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        File dir = layerTarFile.getParentFile();
        File metadataFile = new File(dir, DOCKER_LAYER_METADATA_FILENAME);
        try {
            if (metadataFile.exists()) {
                logger.debug(String.format("%s: %s", metadataFile.getAbsolutePath(), FileUtils.readFileToString(metadataFile, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            logger.debug(String.format("Unable to log contents of %s: %s", metadataFile.getAbsolutePath(), e.getMessage()));
        }
    }

    private void logComponentsPresentAfterAddingThisLayer(final ComponentExtractorFactory componentExtractorFactory, final OperatingSystemEnum currentOs, final File targetImageFileSystemRootDir) throws WrongInspectorOsException {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("Logging components present (so far) after adding this layer");
        if (currentOs == null) {
            logger.debug("Current (running on) OS not provided; cannot determine components present after adding this layer");
            return;
        }
        OperatingSystemEnum inspectorOs = null;
        ImageInfoDerived imageInfoDerived;
        try {
            final ImageInfoParsed imageInfoParsed = parseImageInfo(targetImageFileSystemRootDir);
            inspectorOs = imageInfoParsed.getPkgMgr().getPackageManager().getInspectorOperatingSystem();
            if (!inspectorOs.equals(currentOs)) {
                ImageInspectorOsEnum neededInspectorOs = null;
                try {
                    neededInspectorOs = ImageInspectorOsEnum.getImageInspectorOsEnum(inspectorOs);
                } catch (IntegrationException e) {
                    logger.debug(String.format("Unable to convert OS %s into an inspector OS", inspectorOs.toString()));
                }
                final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs == null ? "<unknown>" : neededInspectorOs.toString());
                throw new WrongInspectorOsException(neededInspectorOs, msg);
            }
            final ComponentExtractor componentExtractor = componentExtractorFactory.createComponentExtractor(gson, imageInfoParsed.getFileSystemRootDir(), null, imageInfoParsed.getPkgMgr().getPackageManager());
            final List<ComponentDetails> comps;
            try {
                comps = componentExtractor.extractComponents(imageInfoParsed.getPkgMgr(), imageInfoParsed.getLinuxDistroName());
            } catch (IntegrationException e) {
                logger.debug(String.format("Unable to log components present after this layer: %s", e.getMessage()));
                return;
            }
            logger.debug(String.format("Found %d components in file system after adding this layer:", comps.size()));
            for (ComponentDetails comp : comps) {
                logger.debug(String.format("\t%s/%s/%s", comp.getName(), comp.getVersion(), comp.getArchitecture()));
            }
        } catch (final PkgMgrDataNotFoundException e) {
            logger.debug(String.format("Unable to log components present after this layer: The file system is not yet populated with the linux distro and package manager files: %s", e.getMessage()));
        } catch (final Exception otherException) {
            logger.debug("Unable to log components present after this layer");
        }
    }
}
