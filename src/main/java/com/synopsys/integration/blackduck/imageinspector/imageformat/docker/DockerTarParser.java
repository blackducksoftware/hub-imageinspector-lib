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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.Manifest;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;
import com.synopsys.integration.blackduck.imageinspector.api.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.exception.IntegrationException;
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


@Component
public class DockerTarParser {
    static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    private static final String DOCKER_LAYER_TAR_FILENAME = "layer.tar";
    private static final String DOCKER_LAYER_METADATA_FILENAME = "json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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

    public ImageInfoParsed extractDockerLayers(final Gson gson, final ComponentExtractorFactory componentExtractorFactory, final OperatingSystemEnum currentOs, final ImageComponentHierarchy imageComponentHierarchy, final File targetImageFileSystemRootDir, final List<File> layerTars, final ManifestLayerMapping manifestLayerMapping) throws IOException, WrongInspectorOsException {
        ImageInfoParsed imageInfoParsed = null;
        int layerIndex = 0;
        for (final String layerDotTarDirname : manifestLayerMapping.getLayers()) {
            logger.trace(String.format("Looking for tar for layer: %s", layerDotTarDirname));
            final File layerTar = getLayerTar(layerTars, layerDotTarDirname);
            if (layerTar != null) {
                extractLayerTarToDir(targetImageFileSystemRootDir, layerTar);
                String layerMetadataFileContents = getLayerMetadataFileContents(layerTar);
                imageInfoParsed = addPostLayerComponents(gson, layerIndex, componentExtractorFactory, currentOs, imageComponentHierarchy,  targetImageFileSystemRootDir, layerMetadataFileContents, manifestLayerMapping.getLayerExternalId(layerIndex));
            } else {
                logger.error(String.format("Could not find the tar for layer %s", layerDotTarDirname));
            }
            layerIndex++;
        }
        List<LayerDetails> layers = imageComponentHierarchy.getLayers();
        int numLayers = layers.size();
        if (numLayers > 0) {
            LayerDetails topLayer = layers.get(numLayers - 1);
            imageComponentHierarchy.setFinalComponents(topLayer.getComponents());
        }
        if (imageInfoParsed == null) {
            imageInfoParsed = new ImageInfoParsed(targetImageFileSystemRootDir, new ImagePkgMgrDatabase(null, PackageManagerEnum.NULL), null);
        }
        return imageInfoParsed;
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

    public ManifestLayerMapping getLayerMapping(final GsonBuilder gsonBuilder, final File workingDirectory, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        logger.debug(String.format("getLayerMappings(): dockerImageName: %s; dockerTagName: %s", dockerImageName, dockerTagName));
        logger.debug(String.format("working dir: %s", workingDirectory));
        final File tarExtractionDirectoryParent = getTarExtractionDirectory(workingDirectory);
        final File tarExtractionDirectory = new File(tarExtractionDirectoryParent, tarFileName);
        final Manifest manifest = manifestFactory.createManifest(tarExtractionDirectoryParent, tarFileName);
        ManifestLayerMapping partialMapping;
        try {
            partialMapping = manifest.getLayerMapping(dockerImageName, dockerTagName);
        } catch (final Exception e) {
            final String msg = String.format("Could not parse the image manifest file : %s", e.getMessage());
            logger.error(msg);
            throw new IntegrationException(msg, e);
        }
        final List<String> layerIds = getLayerIdsFromImageConfigFile(gsonBuilder, tarExtractionDirectory, partialMapping.getConfig());
        if (layerIds == null) {
            return partialMapping;
        }
        return new ManifestLayerMapping(partialMapping, layerIds);
    }

    private List<String> getLayerIdsFromImageConfigFile(final GsonBuilder gsonBuilder, final File tarExtractionDirectory, final String imageConfigFileName) {
        try {
            final File imageConfigFile = new File(tarExtractionDirectory, imageConfigFileName);
            final String imageConfigFileContents = FileUtils
                .readFileToString(imageConfigFile, StandardCharsets.UTF_8);
            logger.debug(String.format("imageConfigFileContents (%s): %s", imageConfigFile.getName(), imageConfigFileContents));
            JsonObject imageConfigJsonObj = gsonBuilder.create().fromJson(imageConfigFileContents, JsonObject.class);
            JsonObject rootFsJsonObj = imageConfigJsonObj.getAsJsonObject("rootfs");
            JsonArray layerIdsJsonArray = rootFsJsonObj.getAsJsonArray("diff_ids");
            final int numLayers = layerIdsJsonArray.size();
            final List<String> layerIds = new ArrayList<>(numLayers);
            for (int i=0; i < numLayers; i++) {
                logger.debug(String.format("layer ID: %s", layerIdsJsonArray.get(i).getAsString()));
                layerIds.add(layerIdsJsonArray.get(i).getAsString());
            }
            return layerIds;
        } catch (Exception e) {
            logger.warn(String.format("Error logging image config file contents: %s", e.getMessage()));
        }
        return null;
    }

    public ImageComponentHierarchy createInitialImageComponentHierarchy(final File workingDirectory, final String tarFileName, final ManifestLayerMapping manifestLayerMapping) throws IntegrationException {
        String manifestFileContents = null;
        String configFileContents = null;
        File tarExtractionDirectory = getTarExtractionDirectory(workingDirectory);
        File tarContentsDirectory = new File(tarExtractionDirectory, tarFileName);
        for (File f : tarContentsDirectory.listFiles()) {
            logger.info(String.format("File %s", f.getName()));
            if ("manifest.json".equals(f.getName())) {
                try {
                    manifestFileContents = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IntegrationException(String.format("Error reading manifest file %s", f.getAbsolutePath()));
                }
            } else if (f.getName().equals(manifestLayerMapping.getConfig())) {
                try {
                    configFileContents = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IntegrationException(String.format("Error reading config file %s", f.getAbsolutePath()));
                }
            }
        }
        return new ImageComponentHierarchy(manifestFileContents, configFileContents);
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

    private ImageInfoParsed addPostLayerComponents(final Gson gson, final int layerIndex, final ComponentExtractorFactory componentExtractorFactory, final OperatingSystemEnum currentOs, final ImageComponentHierarchy imageComponentHierarchy, final File targetImageFileSystemRootDir, final String layerMetadataFileContents, final String layerExternalId) throws WrongInspectorOsException {
        logger.debug("Getting components present (so far) after adding this layer");
        if (currentOs == null) {
            logger.debug("Current (running on) OS not provided; cannot determine components present after adding this layer");
            return null;
        }
        ImageInfoParsed imageInfoParsed = null;
            OperatingSystemEnum inspectorOs;
        try {
            imageInfoParsed = parseImageInfo(targetImageFileSystemRootDir);
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
                return imageInfoParsed;
            }
            logger.debug(String.format("Found %d components in file system after adding this layer:", comps.size()));
            for (ComponentDetails comp : comps) {
                logger.debug(String.format("\t%s/%s/%s", comp.getName(), comp.getVersion(), comp.getArchitecture()));
            }
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, comps);
            imageComponentHierarchy.addLayer(layer);
        } catch (final WrongInspectorOsException wrongOsException) {
            throw wrongOsException;
        } catch (final PkgMgrDataNotFoundException pkgMgrDataNotFoundException) {
            logger.debug(String.format("Unable to collect components present after this layer: The file system is not yet populated with the linux distro and package manager files: %s", pkgMgrDataNotFoundException.getMessage()));
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, null);
            imageComponentHierarchy.addLayer(layer);
        } catch (final Exception otherException) {
            logger.debug("Unable to collect components present after this layer");
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, null);
            imageComponentHierarchy.addLayer(layer);
        }
        return imageInfoParsed;
    }
}
