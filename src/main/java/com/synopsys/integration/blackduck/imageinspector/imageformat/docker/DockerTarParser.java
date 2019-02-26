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

import org.apache.commons.collections.ListUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.PackageManagerToImageInspectorOsMapping;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.Manifest;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class DockerTarParser {
    private static final String DOCKER_LAYER_TAR_FILENAME = "layer.tar";
    private static final String DOCKER_LAYER_METADATA_FILENAME = "json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private CmdExecutor executor;
    private ManifestFactory manifestFactory;
    private Os os;
    private ImageConfigParser imageConfigParser;
    private LayerConfigParser layerConfigParser;
    private FileOperations fileOperations;
    private List<PkgMgr> pkgMgrs;
    private PkgMgrExecutor pkgMgrExecutor;
    private DockerLayerTarExtractor dockerLayerTarExtractor;

    @Autowired
    public void setExecutor(final CmdExecutor executor) {
        this.executor = executor;
    }
    @Autowired
    public void setPkgMgrExecutor(final PkgMgrExecutor pkgMgrExecutor) {
        this.pkgMgrExecutor = pkgMgrExecutor;
    }

    @Autowired
    public void setPkgMgrs(final List<PkgMgr> pkgMgrs) {
        this.pkgMgrs = pkgMgrs;
    }

    @Autowired
    public void setOs(final Os os) {
        this.os = os;
    }

    @Autowired
    public void setManifestFactory(final ManifestFactory manifestFactory) {
        this.manifestFactory = manifestFactory;
    }

    @Autowired
    public void setImageConfigParser(final ImageConfigParser imageConfigParser) {
        this.imageConfigParser = imageConfigParser;
    }

    @Autowired
    public void setLayerConfigParser(final LayerConfigParser layerConfigParser) {
        this.layerConfigParser = layerConfigParser;
    }

    @Autowired
    public void setFileOperations(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    @Autowired
    public void setDockerLayerTarExtractor(final DockerLayerTarExtractor dockerLayerTarExtractor) {
        this.dockerLayerTarExtractor = dockerLayerTarExtractor;
    }

    public List<File> unPackImageTar(final File tarExtractionDirectory, final File dockerTar) throws IOException {
        logger.debug(String.format("tarExtractionDirectory: %s", tarExtractionDirectory));
        fileOperations.logFileOwnerGroupPerms(dockerTar.getParentFile());
        fileOperations.logFileOwnerGroupPerms(dockerTar);
        final List<File> untaredLayerFiles = new ArrayList<>();
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
                            untaredLayerFiles.add(outputFile);
                        }
                    } finally {
                        outputFileStream.close();
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream);
        }
        return untaredLayerFiles;
    }

    public ManifestLayerMapping getLayerMapping(final GsonBuilder gsonBuilder, final File tarExtractionDirectory, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        logger.debug(String.format("getLayerMappings(): dockerImageName: %s; dockerTagName: %s", dockerImageName, dockerTagName));
        logger.debug(String.format("tarExtractionDirectory: %s", tarExtractionDirectory));
        final File tarExtractionSubDirectory = new File(tarExtractionDirectory, tarFileName);
        final Manifest manifest = manifestFactory.createManifest(tarExtractionDirectory, tarFileName);
        ManifestLayerMapping partialMapping;
        try {
            partialMapping = manifest.getLayerMapping(dockerImageName, dockerTagName);
        } catch (final Exception e) {
            final String msg = String.format("Could not parse the image manifest file : %s", e.getMessage());
            logger.error(msg);
            throw new IntegrationException(msg, e);
        }
        final List<String> externalLayerIds = getExternalLayerIdsFromImageConfigFile(gsonBuilder, tarExtractionSubDirectory, partialMapping.getImageConfigFilename());
        if (externalLayerIds == null) {
            return partialMapping;
        }
        return new ManifestLayerMapping(partialMapping, externalLayerIds);
    }

    public ImageComponentHierarchy createInitialImageComponentHierarchy(final File tarExtractionDirectory, final String tarFileName, final ManifestLayerMapping manifestLayerMapping) throws IntegrationException {
        String manifestFileContents = null;
        String configFileContents = null;
        File tarContentsDirectory = new File(tarExtractionDirectory, tarFileName);
        for (File tarFileContentsFile : fileOperations.listFilesInDir(tarContentsDirectory)) {
            logger.debug(String.format("File %s", tarFileContentsFile.getName()));
            if ("manifest.json".equals(tarFileContentsFile.getName())) {
                try {
                    manifestFileContents = FileUtils.readFileToString(tarFileContentsFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IntegrationException(String.format("Error reading manifest file %s", tarFileContentsFile.getAbsolutePath()));
                }
            } else if (tarFileContentsFile.getName().equals(manifestLayerMapping.getImageConfigFilename())) {
                try {
                    configFileContents = FileUtils.readFileToString(tarFileContentsFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IntegrationException(String.format("Error reading config file %s", tarFileContentsFile.getAbsolutePath()));
                }
            }
        }
        return new ImageComponentHierarchy(manifestFileContents, configFileContents);
    }

    public ImageInfoParsed extractImageLayers(final GsonBuilder gsonBuilder, final ImageInspectorOsEnum currentOs, final ImageComponentHierarchy imageComponentHierarchy,
        final File containerFileSystemRootDir, final List<File> layerTars, final ManifestLayerMapping manifestLayerMapping,
        final String platformTopLayerExternalId) throws IOException, WrongInspectorOsException {
        ImageInfoParsed imageInfoParsed = null;
        int layerIndex = 0;
        for (final String layerDotTarDirname : manifestLayerMapping.getLayerInternalIds()) {
            logger.trace(String.format("Looking for tar for layer: %s", layerDotTarDirname));
            final File layerTar = getLayerTar(layerTars, layerDotTarDirname);
            if (layerTar != null) {
                extractLayerTarToDir(containerFileSystemRootDir, layerTar);
                final String layerMetadataFileContents = getLayerMetadataFileContents(layerTar);
                final List<String> layerCmd = layerConfigParser.parseCmd(gsonBuilder, layerMetadataFileContents);
                final boolean isPlatformTopLayer = isThisThePlatformTopLayer(manifestLayerMapping, platformTopLayerExternalId, layerIndex);
                if (isPlatformTopLayer) {
                    imageComponentHierarchy.setPlatformTopLayerIndex(layerIndex);
                }
                imageInfoParsed = addPostLayerComponents(layerIndex, currentOs, imageComponentHierarchy, containerFileSystemRootDir, layerMetadataFileContents, layerCmd,
                    manifestLayerMapping.getLayerExternalId(layerIndex), isPlatformTopLayer);
                if (isPlatformTopLayer) {
                    logger.info(String.format("Layer %s is the top layer of the platform. Components present after adding this layer will be omitted from results", platformTopLayerExternalId));
                }
            } else {
                logger.error(String.format("Could not find the tar for layer %s", layerDotTarDirname));
            }
            layerIndex++;
        }
        List<LayerDetails> layers = imageComponentHierarchy.getLayers();
        int numLayers = layers.size();
        if (numLayers > 0) {
            LayerDetails topLayer = layers.get(numLayers - 1);
            final List<ComponentDetails> netComponents = getNetComponents(topLayer.getComponents(), imageComponentHierarchy.getPlatformComponents());
            imageComponentHierarchy.setFinalComponents(netComponents);
        }
        if (imageInfoParsed == null) {
            imageInfoParsed = new ImageInfoParsed(containerFileSystemRootDir, new ImagePkgMgrDatabase(null, PackageManagerEnum.NULL), null, null);
        }
        return imageInfoParsed;
    }

    private boolean isThisThePlatformTopLayer(final ManifestLayerMapping manifestLayerMapping, final String platformTopLayerExternalId, final int layerIndex) {
        final String currentLayerExternalId = manifestLayerMapping.getLayerExternalId(layerIndex);
        boolean isTop = (platformTopLayerExternalId != null) && platformTopLayerExternalId.equals(currentLayerExternalId);
        logger.debug(String.format("Results of test for top of platform: layerIndex: %d, platformTopLayerExternalId: %s, currentLayerExternalId: %s, isTop: %b", layerIndex, platformTopLayerExternalId, currentLayerExternalId, isTop));
        return isTop;
    }

    private List<ComponentDetails> getNetComponents(final List<ComponentDetails> grossComponents, final List<ComponentDetails> componentsToOmit) {
        logger.info(String.format("There are %d components to omit", componentsToOmit.size()));
        if (componentsToOmit == null || componentsToOmit.isEmpty()) {
            return grossComponents;
        }
        List<ComponentDetails> netComponents = ListUtils.subtract(grossComponents, componentsToOmit);
        logger.debug(String.format("grossComponents: %d, componentsToOmit: %d, netComponents: %d", grossComponents.size(), componentsToOmit.size(), netComponents.size()));
        return netComponents;
    }

    private List<String> getExternalLayerIdsFromImageConfigFile(final GsonBuilder gsonBuilder, final File tarExtractionDirectory, final String imageConfigFileName) {
        try {
            final File imageConfigFile = new File(tarExtractionDirectory, imageConfigFileName);
            final String imageConfigFileContents = fileOperations
                                                       .readFileToString(imageConfigFile);
            logger.debug(String.format("imageConfigFileContents (%s): %s", imageConfigFile.getName(), imageConfigFileContents));
            final List<String> externalLayerIds = imageConfigParser.parseExternalLayerIds(gsonBuilder, imageConfigFileContents);
            return externalLayerIds;
        } catch (Exception e) {
            logger.warn(String.format("Error logging image config file contents: %s", e.getMessage()));
        }
        return null;
    }

    private Optional<String> extractLinuxDistroNameFromFileSystem(final File targetImageFileSystemRootDir) {
        final LinuxFileSystem extractedFileSys = new LinuxFileSystem(targetImageFileSystemRootDir, fileOperations);
        final Optional<File> etcDir = extractedFileSys.getEtcDir();
        if (!etcDir.isPresent()) {
            return Optional.empty();
        }
        return extractLinuxDistroNameFromEtcDir(etcDir.get());
    }

    private Optional<String> extractLinuxDistroNameFromEtcDir(final File etcDir) {
        logger.trace(String.format("/etc directory: %s", etcDir.getAbsolutePath()));
        if (fileOperations.listFilesInDir(etcDir).length == 0) {
            logger.warn(String.format("Could not determine the Operating System because the /etc dir (%s) is empty", etcDir.getAbsolutePath()));
        }
        return extractLinuxDistroNameFromFiles(fileOperations.listFilesInDir(etcDir));
    }

    private Optional<String> extractLinuxDistroNameFromFiles(final File[] etcFiles) {
        for (final File etcFile : etcFiles) {
            if (os.isLinuxDistroFile(etcFile)) {
                return os.getLinxDistroName(etcFile);
            }
        }
        return Optional.empty();
    }

    private File extractLayerTarToDir(final File containerFileSystemRootDir, final File layerTar) throws IOException {
        logger.trace(String.format("Extracting layer: %s into %s", layerTar.getAbsolutePath(), containerFileSystemRootDir.getAbsolutePath()));
        final List<File> filesToRemove = dockerLayerTarExtractor.extractLayerTarToDir(fileOperations, layerTar, containerFileSystemRootDir);
        for (final File fileToRemove : filesToRemove) {
            if (fileToRemove.isDirectory()) {
                logger.debug(String.format("Removing dir marked for deletion: %s", fileToRemove.getAbsolutePath()));
                FileUtils.deleteDirectory(fileToRemove);
            } else {
                logger.debug(String.format("Removing file marked for deletion: %s", fileToRemove.getAbsolutePath()));
                fileOperations.deleteQuietly(fileToRemove);
            }
        }
        return containerFileSystemRootDir;
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

    private String getLayerMetadataFileContents(final File layerTarFile) {
        String layerMetadataFileContents = null;
        File dir = layerTarFile.getParentFile();
        File metadataFile = new File(dir, DOCKER_LAYER_METADATA_FILENAME);
        try {
            if (metadataFile.exists()) {
                layerMetadataFileContents = FileUtils.readFileToString(metadataFile, StandardCharsets.UTF_8);
                logger.debug(String.format("%s: %s", metadataFile.getAbsolutePath(), layerMetadataFileContents));
            }
        } catch (IOException e) {
            logger.debug(String.format("Unable to log contents of %s: %s", metadataFile.getAbsolutePath(), e.getMessage()));
        }
        return layerMetadataFileContents;
    }

    private ImageInfoParsed addPostLayerComponents(final int layerIndex, final ImageInspectorOsEnum currentOs,
        final ImageComponentHierarchy imageComponentHierarchy, final File targetImageFileSystemRootDir, final String layerMetadataFileContents,
        final List<String> layerCmd, final String layerExternalId,
        boolean isPlatformTopLayer) throws WrongInspectorOsException {
        logger.debug(String.format("Getting components present (so far) after adding layer %s", layerExternalId));
        if (currentOs == null) {
            logger.debug(String.format("Current (running on) OS not provided; cannot determine components present after adding layer %s", layerExternalId));
            return null;
        }
        ImageInfoParsed imageInfoParsed = null;
        ImageInspectorOsEnum neededInspectorOs;
        try {
            imageInfoParsed = parseImageInfo(targetImageFileSystemRootDir);
            neededInspectorOs = PackageManagerToImageInspectorOsMapping
                                    .getImageInspectorOs(imageInfoParsed.getImagePkgMgrDatabase().getPackageManager());
            if (!neededInspectorOs.equals(currentOs)) {
                final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs == null ? "<unknown>" : neededInspectorOs.toString());
                throw new WrongInspectorOsException(neededInspectorOs, msg);
            }
            final List<ComponentDetails> comps;
            try {
                final String[] pkgMgrOutputLines = pkgMgrExecutor.runPackageManager(executor, imageInfoParsed.getPkgMgr(), imageInfoParsed.getImagePkgMgrDatabase());
                comps = imageInfoParsed.getPkgMgr().extractComponentsFromPkgMgrOutput(imageInfoParsed.getFileSystemRootDir(), imageInfoParsed.getLinuxDistroName(), pkgMgrOutputLines);
            } catch (IntegrationException e) {
                logger.debug(String.format("Unable to log components present after layer %s: %s", layerExternalId, e.getMessage()));
                return imageInfoParsed;
            }
            logger.info(String.format("Found %d components in file system after adding layer %s (cmd: %s):", comps.size(), layerExternalId, layerCmd));
            for (ComponentDetails comp : comps) {
                logger.debug(String.format("\t%s/%s/%s", comp.getName(), comp.getVersion(), comp.getArchitecture()));
            }
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd, comps);
            imageComponentHierarchy.addLayer(layer);
            if (isPlatformTopLayer) {
                imageComponentHierarchy.setPlatformComponents(comps);
            }
        } catch (final WrongInspectorOsException wrongOsException) {
            throw wrongOsException;
        } catch (final PkgMgrDataNotFoundException pkgMgrDataNotFoundException) {
            logger.debug(String.format("Unable to collect components present after layer %s: The file system is not yet populated with the linux distro and package manager files: %s", layerExternalId, pkgMgrDataNotFoundException.getMessage()));
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd,  null);
            imageComponentHierarchy.addLayer(layer);
        } catch (final Exception otherException) {
            logger.debug(String.format("Unable to collect components present after layer %s", layerExternalId));
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd,  null);
            imageComponentHierarchy.addLayer(layer);
        }
        return imageInfoParsed;
    }

    ImageInfoParsed parseImageInfo(final File targetImageFileSystemRootDir) throws PkgMgrDataNotFoundException {
        if (pkgMgrs == null) {
            logger.error("No pmgMgrs configured");
        } else {
            logger.trace(String.format("pkgMgrs.size(): %d", pkgMgrs.size()));
            for (PkgMgr pkgMgr : pkgMgrs) {
                if (pkgMgr.isApplicable(targetImageFileSystemRootDir)) {
                    final ImagePkgMgrDatabase targetImagePkgMgr = new ImagePkgMgrDatabase(pkgMgr.getImagePackageManagerDirectory(targetImageFileSystemRootDir),
                        pkgMgr.getType());
                    final String linuxDistroName = extractLinuxDistroNameFromFileSystem(targetImageFileSystemRootDir).orElse(null);
                    final ImageInfoParsed imagePkgMgrInfo = new ImageInfoParsed(targetImageFileSystemRootDir, targetImagePkgMgr, linuxDistroName, pkgMgr);
                    return imagePkgMgrInfo;
                }
            }
        }
        throw new PkgMgrDataNotFoundException("No package manager database found in this Docker image.");
    }
}
