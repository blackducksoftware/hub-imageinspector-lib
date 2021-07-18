/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.lib.*;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.PackageManagerToImageInspectorOsMapping;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class DockerTarParser {
    private static final String DOCKER_LAYER_METADATA_FILENAME = "json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private CmdExecutor executor;
    private DockerManifestFactory dockerManifestFactory;
    private Os os;
    private DockerImageConfigParser dockerImageConfigParser;
    private DockerLayerConfigParser dockerLayerConfigParser;
    private FileOperations fileOperations;
    private List<PkgMgr> pkgMgrs;
    private PkgMgrExecutor pkgMgrExecutor;
    private DockerLayerTarExtractor dockerLayerTarExtractor;
    private PkgMgrExtractor pkgMgrExtractor;
    private PackageGetter packageGetter;

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
    public void setManifestFactory(final DockerManifestFactory dockerManifestFactory) {
        this.dockerManifestFactory = dockerManifestFactory;
    }

    @Autowired
    public void setImageConfigParser(final DockerImageConfigParser dockerImageConfigParser) {
        this.dockerImageConfigParser = dockerImageConfigParser;
    }

    @Autowired
    public void setLayerConfigParser(final DockerLayerConfigParser dockerLayerConfigParser) {
        this.dockerLayerConfigParser = dockerLayerConfigParser;
    }

    @Autowired
    public void setFileOperations(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    @Autowired
    public void setDockerLayerTarExtractor(final DockerLayerTarExtractor dockerLayerTarExtractor) {
        this.dockerLayerTarExtractor = dockerLayerTarExtractor;
    }

    @Autowired
    public void setPkgMgrExtractor(PkgMgrExtractor pkgMgrExtractor) {
        this.pkgMgrExtractor = pkgMgrExtractor;
    }

    @Autowired
    public void setPackageGetter(PackageGetter packageGetter) {
        this.packageGetter = packageGetter;
    }

    public ContainerFileSystemWithPkgMgrDb extractPkgMgrDb(ImageInspectorOsEnum currentOs, final String targetLinuxDistroOverride,
                                                           final ContainerFileSystem containerFileSystem, final List<TypedArchiveFile> layerTars, final ManifestLayerMapping manifestLayerMapping,
                                                           final String platformTopLayerExternalId) throws IOException, WrongInspectorOsException {
        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy();
        ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = null;
        int layerIndex = 0;
        boolean inApplicationLayers = false;
        for (final String layerDotTarDirname : manifestLayerMapping.getLayerInternalIds()) {
            logger.trace(String.format("Looking for tar for layer: %s", layerDotTarDirname));
            final TypedArchiveFile layerTar = getLayerTar(layerTars, layerDotTarDirname);
            if (layerTar != null) {
                extractLayerTarToDir(containerFileSystem.getTargetImageFileSystemFull(), layerTar);
                if (inApplicationLayers && containerFileSystem.getTargetImageFileSystemAppOnly().isPresent()) {
                    extractLayerTarToDir(containerFileSystem.getTargetImageFileSystemAppOnly().get(), layerTar);
                }
                final String layerMetadataFileContents = getLayerMetadataFileContents(layerTar);
                final List<String> layerCmd = dockerLayerConfigParser.parseCmd(layerMetadataFileContents);
                final boolean isPlatformTopLayer = isThisThePlatformTopLayer(manifestLayerMapping, platformTopLayerExternalId, layerIndex);
                if (isPlatformTopLayer) {
                    imageComponentHierarchy.setPlatformTopLayerIndex(layerIndex);
                    inApplicationLayers = true; // will be true next iteration
                    logger.info(String.format("Layer %d is the top layer of the platform. Components present after adding this layer will be omitted from results", layerIndex));
                }
                containerFileSystemWithPkgMgrDb = addPostLayerComponents(layerIndex, currentOs, targetLinuxDistroOverride, containerFileSystemWithPkgMgrDb, imageComponentHierarchy, containerFileSystem, layerMetadataFileContents, layerCmd,
                    manifestLayerMapping.getLayerExternalId(layerIndex), isPlatformTopLayer);
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
        if (containerFileSystemWithPkgMgrDb == null) {
            containerFileSystemWithPkgMgrDb = new ContainerFileSystemWithPkgMgrDb(containerFileSystem, new ImagePkgMgrDatabase(null, PackageManagerEnum.NULL), targetLinuxDistroOverride, null, imageComponentHierarchy);
        }
        return containerFileSystemWithPkgMgrDb;
    }

    private boolean isThisThePlatformTopLayer(final ManifestLayerMapping manifestLayerMapping, final String platformTopLayerExternalId, final int layerIndex) {
        final String currentLayerExternalId = manifestLayerMapping.getLayerExternalId(layerIndex);
        boolean isTop = (platformTopLayerExternalId != null) && platformTopLayerExternalId.equals(currentLayerExternalId);
        logger.trace(String.format("Results of test for top of platform: layerIndex: %d, platformTopLayerExternalId: %s, currentLayerExternalId: %s, isTop: %b", layerIndex, platformTopLayerExternalId, currentLayerExternalId, isTop));
        return isTop;
    }

    private List<ComponentDetails> getNetComponents(final List<ComponentDetails> grossComponents, final List<ComponentDetails> componentsToOmit) {
        logger.info(String.format("There are %d components to omit", componentsToOmit.size()));
        if (componentsToOmit.isEmpty()) {
            return grossComponents;
        }
        List<ComponentDetails> netComponents = ListUtils.subtract(grossComponents, componentsToOmit);
        logger.debug(String.format("grossComponents: %d, componentsToOmit: %d, netComponents: %d", grossComponents.size(), componentsToOmit.size(), netComponents.size()));
        return netComponents;
    }

    private void extractLayerTarToDir(final File destinationDir, final TypedArchiveFile layerTar) throws IOException {
        logger.trace(String.format("Extracting layer: %s into %s", layerTar.getFile().getAbsolutePath(), destinationDir.getAbsolutePath()));
        final List<File> filesToRemove = dockerLayerTarExtractor.extractLayerTarToDir(fileOperations, layerTar.getFile(), destinationDir);
        for (final File fileToRemove : filesToRemove) {
            if (fileToRemove.isDirectory()) {
                logger.trace(String.format("Removing dir marked for deletion: %s", fileToRemove.getAbsolutePath()));
                FileUtils.deleteDirectory(fileToRemove);
            } else {
                logger.trace(String.format("Removing file marked for deletion: %s", fileToRemove.getAbsolutePath()));
                fileOperations.deleteQuietly(fileToRemove);
            }
        }
    }

    private TypedArchiveFile getLayerTar(final List<TypedArchiveFile> layerTars, final String layer) {
        TypedArchiveFile layerTar = null;
        for (final TypedArchiveFile candidateLayerTar : layerTars) {
            if (layer.equals(candidateLayerTar.getFile().getParentFile().getName())) {
                logger.trace(String.format("Found layer tar for layer %s", layer));
                layerTar = candidateLayerTar;
                break;
            }
        }
        return layerTar;
    }

    private String getLayerMetadataFileContents(final TypedArchiveFile layerTarFile) {
        String layerMetadataFileContents = null;
        File dir = layerTarFile.getFile().getParentFile();
        File metadataFile = new File(dir, DOCKER_LAYER_METADATA_FILENAME);
        try {
            if (metadataFile.exists()) {
                layerMetadataFileContents = FileUtils.readFileToString(metadataFile, StandardCharsets.UTF_8);
                logger.trace(String.format("%s: %s", metadataFile.getAbsolutePath(), layerMetadataFileContents));
            }
        } catch (IOException e) {
            logger.trace(String.format("Unable to log contents of %s: %s", metadataFile.getAbsolutePath(), e.getMessage()));
        }
        return layerMetadataFileContents;
    }

    private ContainerFileSystemWithPkgMgrDb addPostLayerComponents(final int layerIndex, final ImageInspectorOsEnum currentOs, final String targetLinuxDistroOverride, ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb,
                                                                   final ImageComponentHierarchy imageComponentHierarchy, final ContainerFileSystem containerFileSystem, final String layerMetadataFileContents,
                                                                   final List<String> layerCmd, final String layerExternalId,
                                                                   boolean isPlatformTopLayer) throws WrongInspectorOsException {
        logger.debug(String.format("Getting components present (so far) after adding layer %d", layerIndex));
        logger.trace(String.format("Layer ID: %s", layerExternalId));
        if (currentOs == null) {
            logger.debug(String.format("Current (running on) OS not provided; cannot determine components present after adding layer %d", layerIndex));
            return null;
        }
        try {
            if (containerFileSystemWithPkgMgrDb == null) {
                logger.debug("Attempting to determine the target image package manager");
                containerFileSystemWithPkgMgrDb = pkgMgrExtractor.extract(containerFileSystem, targetLinuxDistroOverride, imageComponentHierarchy);
                final ImageInspectorOsEnum neededInspectorOs = PackageManagerToImageInspectorOsMapping
                                        .getImageInspectorOs(containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getPackageManager());
                if (!neededInspectorOs.equals(currentOs)) {
                    final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs.toString());
                    throw new WrongInspectorOsException(neededInspectorOs, msg);
                }
            } else {
                logger.debug(String.format("The target image package manager has previously been determined: %s", containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getPackageManager().toString()));
            }
            logger.info("Querying pkg mgr for components after adding layer {}", layerIndex);
            final List<ComponentDetails> comps = packageGetter.queryPkgMgrForDependencies(containerFileSystemWithPkgMgrDb);
            if (comps.isEmpty()) {
                return containerFileSystemWithPkgMgrDb;
            }
            logger.info(String.format("Found %d components in file system after adding layer %d", comps.size(), layerIndex));
            for (ComponentDetails comp : comps) {
                logger.trace(String.format("\t%s/%s/%s", comp.getName(), comp.getVersion(), comp.getArchitecture()));
            }
            final LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd, comps);
            imageComponentHierarchy.addLayer(layer);
            if (isPlatformTopLayer) {
                imageComponentHierarchy.setPlatformComponents(comps);
            }
        } catch (final WrongInspectorOsException wrongOsException) {
            throw wrongOsException;
        } catch (final PkgMgrDataNotFoundException pkgMgrDataNotFoundException) {
            logger.debug(String.format("Unable to collect components present after layer %d: The file system is not yet populated with the linux distro and package manager files: %s", layerIndex, pkgMgrDataNotFoundException.getMessage()));
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd,  null);
            imageComponentHierarchy.addLayer(layer);
        } catch (final Exception otherException) {
            logger.debug(String.format("Unable to collect components present after layer %d", layerIndex));
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd,  null);
            imageComponentHierarchy.addLayer(layer);
        }
        return containerFileSystemWithPkgMgrDb;
    }
}
