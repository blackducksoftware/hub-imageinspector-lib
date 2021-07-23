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
import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.lib.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.PackageManagerToImageInspectorOsMapping;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;

@Component
public class DockerTarParser {
    private static final String DOCKER_LAYER_METADATA_FILENAME = "json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DockerLayerConfigParser dockerLayerConfigParser;
    private FileOperations fileOperations;
    private DockerLayerTarExtractor dockerLayerTarExtractor;
    private PkgMgrDbExtractor pkgMgrDbExtractor;
    private PackageGetter packageGetter;

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
    public void setPkgMgrExtractor(PkgMgrDbExtractor pkgMgrDbExtractor) {
        this.pkgMgrDbExtractor = pkgMgrDbExtractor;
    }

    @Autowired
    public void setPackageGetter(PackageGetter packageGetter) {
        this.packageGetter = packageGetter;
    }

    // TODO make sure there's test coverage for these new methods:

    // TODO this is Docker format specific
    // but it was dumb; method below it makes more sense; remove this one
    public TypedArchiveFile selectLayerTar(List<TypedArchiveFile> unOrderedLayerTars, ManifestLayerMapping manifestLayerMapping, int layerIndex) {
        String layerInternalId = manifestLayerMapping.getLayerInternalIds().get(layerIndex);
        return getLayerTar(unOrderedLayerTars, layerInternalId);
    }

    // TODO this is Docker format specific
    public List<TypedArchiveFile> getOrderedLayerTars(List<TypedArchiveFile> unOrderedLayerTars, ManifestLayerMapping manifestLayerMapping) {
        List<TypedArchiveFile> orderedLayerTars = new ArrayList<>(manifestLayerMapping.getLayerInternalIds().size());
        for (String layerInternalId : manifestLayerMapping.getLayerInternalIds()) {
            orderedLayerTars.add(getLayerTar(unOrderedLayerTars, layerInternalId));
        }
        return orderedLayerTars;
    }

    // TODO image format independent
    public void extractLayerTar(File destinationDir, final TypedArchiveFile layerTar) throws IOException, WrongInspectorOsException {
        // TODO eventually inline this method:
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

    // TODO this should be image format independent
    public Optional<Integer> getPlatformTopLayerIndex(FullLayerMapping fullLayerMapping, @Nullable String platformTopLayerExternalId) {
        if (platformTopLayerExternalId != null) {
            int curLayerIndex = 0;
            for (String candidateLayerExternalId : fullLayerMapping.getLayerExternalIds()) {
                if ((candidateLayerExternalId != null) && (candidateLayerExternalId.equals(platformTopLayerExternalId))) {
                    logger.trace("Found platform top layer ({}) at layerIndex: {}", platformTopLayerExternalId, curLayerIndex);
                    return Optional.of(curLayerIndex);
                }
                curLayerIndex++;
            }
        }
        return Optional.empty();
    }

    // TODO should be image format independent
    public LayerMetadata getLayerMetadata(FullLayerMapping fullLayerMapping, TypedArchiveFile layerTar, int layerIndex) {
        final String layerMetadataFileContents = getLayerMetadataFileContents(layerTar);
        final List<String> layerCmd = dockerLayerConfigParser.parseCmd(layerMetadataFileContents);
        String layerExternalId = fullLayerMapping.getLayerExternalId(layerIndex);
        return new LayerMetadata(layerExternalId, layerCmd);
    }

    public LayerComponents getLayerComponents(ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, LayerMetadata layerMetadata) {
        final List<ComponentDetails> components = packageGetter.queryPkgMgrForDependencies(containerFileSystemWithPkgMgrDb);
        return new LayerComponents(layerMetadata, components);
    }

    public Optional<ImageInspectorOsEnum> determineNeededImageInspectorOs(ContainerFileSystem containerFileSystem,
                                                                          String targetLinuxDistroOverride) {
        logger.debug("Attempting to determine the target image package manager");
        ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = null;
        try {
            containerFileSystemWithPkgMgrDb = pkgMgrDbExtractor.extract(containerFileSystem, targetLinuxDistroOverride);
        } catch (PkgMgrDataNotFoundException e) {
            return Optional.empty();
        }
        final ImageInspectorOsEnum neededInspectorOs = PackageManagerToImageInspectorOsMapping
                .getImageInspectorOs(containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getPackageManager());
        return Optional.of(neededInspectorOs);
    }

    // TODO this is Docker format specific
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

    // TODO this is not Docker specific
    public void checkInspectorOs(ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, ImageInspectorOsEnum currentOs) throws WrongInspectorOsException {
        final ImageInspectorOsEnum neededInspectorOs = PackageManagerToImageInspectorOsMapping
                .getImageInspectorOs(containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getPackageManager());
        if (!neededInspectorOs.equals(currentOs)) {
            final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs);
            throw new WrongInspectorOsException(neededInspectorOs, msg);
        }
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
}
