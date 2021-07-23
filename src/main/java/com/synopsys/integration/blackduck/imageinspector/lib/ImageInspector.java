/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ComponentHierarchyBuilder;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ImageLayerApplier;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerImageDirectory;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.TarOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;

// As support for other image formats is added, this class will manage the list of TarParsers
@Component
public class ImageInspector {
    public static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    public static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private static final String NO_PKG_MGR_FOUND = "noPkgMgr";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DockerTarParser tarParser;
    private final TarOperations tarOperations;
    private final GsonBuilder gsonBuilder;
    private final FileOperations fileOperations;
    private final DockerImageConfigParser dockerImageConfigParser;
    private final DockerManifestFactory dockerManifestFactory;
    private final List<PkgMgr> pkgMgrs;
    private final PkgMgrDbExtractor pkgMgrDbExtractor;
    private final ImageLayerApplier imageLayerApplier;

    public ImageInspector(List<PkgMgr> pkgMgrs, PkgMgrDbExtractor pkgMgrDbExtractor, DockerTarParser tarParser, TarOperations tarOperations, GsonBuilder gsonBuilder,
                          FileOperations fileOperations, DockerImageConfigParser dockerImageConfigParser, DockerManifestFactory dockerManifestFactory,
                          ImageLayerApplier imageLayerApplier) {

        this.pkgMgrs = pkgMgrs;
        this.pkgMgrDbExtractor = pkgMgrDbExtractor;
        this.tarParser = tarParser;
        this.tarOperations = tarOperations;
        this.gsonBuilder = gsonBuilder;
        this.fileOperations = fileOperations;
        this.dockerImageConfigParser = dockerImageConfigParser;
        this.dockerManifestFactory = dockerManifestFactory;
        this.imageLayerApplier = imageLayerApplier;
    }

    public File getTarExtractionDirectory(final File workingDirectory) {
        return new File(workingDirectory, TAR_EXTRACTION_DIRECTORY);
    }

    public DockerImageDirectory extractImageTar(final File tarExtractionDirectory, final File dockerTar) throws IOException {
        File imageDir = tarOperations.extractTarToGivenDir(tarExtractionDirectory, dockerTar);
        return new DockerImageDirectory(gsonBuilder, fileOperations, dockerImageConfigParser, dockerManifestFactory, imageDir);
    }

    public ContainerFileSystemWithPkgMgrDb extractDockerLayers(final ImageInspectorOsEnum currentOs, final String targetLinuxDistroOverride, final ContainerFileSystem containerFileSystem, final List<TypedArchiveFile> orderedLayerTars,
                                                               final FullLayerMapping layerMapping, final String platformTopLayerExternalId,
                                                               ComponentHierarchyBuilder componentHierarchyBuilder) throws IOException, WrongInspectorOsException {

        Optional<Integer> platformTopLayerIndex = tarParser.getPlatformTopLayerIndex(layerMapping, platformTopLayerExternalId);
        if (platformTopLayerIndex.isPresent()) {
            componentHierarchyBuilder.setPlatformTopLayerIndex(platformTopLayerIndex.get());
        }

        ContainerFileSystemWithPkgMgrDb postLayerContainerFileSystemWithPkgMgrDb = null;
        boolean inApplicationLayers = false;
        int layerIndex = 0;
        for (TypedArchiveFile layerTar : orderedLayerTars) {
            imageLayerApplier.extractLayerTar(containerFileSystem.getTargetImageFileSystemFull(), layerTar);
            if (inApplicationLayers && containerFileSystem.getTargetImageFileSystemAppOnly().isPresent()) {
                imageLayerApplier.extractLayerTar(containerFileSystem.getTargetImageFileSystemAppOnly().get(), layerTar);
            }
            LayerMetadata layerMetadata = tarParser.getLayerMetadata(layerMapping, layerTar, layerIndex);
            try {
                postLayerContainerFileSystemWithPkgMgrDb = pkgMgrDbExtractor.extract(containerFileSystem, targetLinuxDistroOverride);
                componentHierarchyBuilder.addLayer(postLayerContainerFileSystemWithPkgMgrDb, layerIndex, layerMetadata.getLayerExternalId(), layerMetadata.getLayerCmd());
            } catch (PkgMgrDataNotFoundException pkgMgrDataNotFoundException) {
                logger.debug(String.format("Unable to collect components present after layer %d: The file system is not yet populated with the linux distro and package manager files: %s", layerIndex, pkgMgrDataNotFoundException.getMessage()));
            }
            if (platformTopLayerIndex.isPresent() && (layerIndex == platformTopLayerIndex.get())) {
                inApplicationLayers = true; // for subsequent iterations
            }
            layerIndex++;
        }
        // Never did find a pkg mgr, so create the result without one
        if (postLayerContainerFileSystemWithPkgMgrDb == null) {
            postLayerContainerFileSystemWithPkgMgrDb = new ContainerFileSystemWithPkgMgrDb(containerFileSystem, new ImagePkgMgrDatabase(null, PackageManagerEnum.NULL), targetLinuxDistroOverride, null);
        } else {
            tarParser.checkInspectorOs(postLayerContainerFileSystemWithPkgMgrDb, currentOs);
        }
        // TODO This has always returned null in some cases, like a scratch image or a windows image, so should be OK w/out a null check
        return postLayerContainerFileSystemWithPkgMgrDb;
        //OLD: return tarParser.extractPkgMgrDb(currentOs, targetLinuxDistroOverride, containerFileSystem, unOrderedLayerTars, layerMapping, platformTopLayerExternalId);
    }

    public ImageInfoDerived generateBdioFromGivenComponents(final BdioGenerator bdioGenerator, ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, final FullLayerMapping mapping,
                                                            ImageComponentHierarchy imageComponentHierarchy,
                                                            final String projectName,
                                                            final String versionName,
                                                            final String codeLocationPrefix,
                                                            final boolean organizeComponentsByLayer,
                                                            final boolean includeRemovedComponents,
                                                            final boolean platformComponentsExcluded) {
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(mapping, projectName, versionName, codeLocationPrefix, containerFileSystemWithPkgMgrDb, imageComponentHierarchy, platformComponentsExcluded);
        final SimpleBdioDocument bdioDocument = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy(imageInfoDerived.getCodeLocationName(),
            imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getImageInfoParsed().getLinuxDistroName(), imageComponentHierarchy, organizeComponentsByLayer,
            includeRemovedComponents);
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    private ImageInfoDerived deriveImageInfo(final FullLayerMapping mapping, final String projectName, final String versionName,
                                             final String codeLocationPrefix, final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, ImageComponentHierarchy imageComponentHierarchy,
                                             final boolean platformComponentsExcluded) {
        logger.debug(String.format("deriveImageInfo(): projectName: %s, versionName: %s", projectName, versionName));
        final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(containerFileSystemWithPkgMgrDb, imageComponentHierarchy);
        imageInfoDerived.setFullLayerMapping(mapping);
        imageInfoDerived.setCodeLocationName(deriveCodeLocationName(codeLocationPrefix, imageInfoDerived, platformComponentsExcluded));
        imageInfoDerived.setFinalProjectName(deriveBlackDuckProject(imageInfoDerived.getFullLayerMapping().getManifestLayerMapping().getImageName(), projectName, platformComponentsExcluded));
        imageInfoDerived.setFinalProjectVersionName(deriveBlackDuckProjectVersion(imageInfoDerived.getFullLayerMapping(), versionName));
        logger.info(String.format("Black Duck project: %s, version: %s; Code location : %s", imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getCodeLocationName()));
        return imageInfoDerived;
    }

    private String deriveCodeLocationName(final String codeLocationPrefix, final ImageInfoDerived imageInfoDerived, final boolean platformComponentsExcluded) {
        final String pkgMgrName = derivePackageManagerName(imageInfoDerived);
        return Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getFullLayerMapping().getManifestLayerMapping().getImageName(), imageInfoDerived.getFullLayerMapping().getManifestLayerMapping().getTagName(),
                pkgMgrName, platformComponentsExcluded);
    }

    private String derivePackageManagerName(final ImageInfoDerived imageInfoDerived) {
        final String pkgMgrName;
        final ImagePkgMgrDatabase imagePkgMgrDatabase = imageInfoDerived.getImageInfoParsed().getImagePkgMgrDatabase();
        if (imagePkgMgrDatabase != null && imagePkgMgrDatabase.getPackageManager() != PackageManagerEnum.NULL) {
            pkgMgrName = imageInfoDerived.getImageInfoParsed().getImagePkgMgrDatabase().getPackageManager().toString();
        } else {
            pkgMgrName = NO_PKG_MGR_FOUND;
        }
        return pkgMgrName;
    }

    private String deriveBlackDuckProject(final String imageName, final String projectName,
        final boolean platformComponentsExcluded) {
        String blackDuckProjectName;
        if (StringUtils.isBlank(projectName)) {
            blackDuckProjectName = Names.getBlackDuckProjectNameFromImageName(imageName, platformComponentsExcluded);
        } else {
            logger.debug("Using project from config property");
            blackDuckProjectName = projectName;
        }
        return blackDuckProjectName;
    }

    private String deriveBlackDuckProjectVersion(final FullLayerMapping mapping, final String versionName) {
        String blackDuckVersionName;
        if (StringUtils.isBlank(versionName)) {
            blackDuckVersionName = mapping.getManifestLayerMapping().getTagName();
        } else {
            logger.debug("Using project version from config property");
            blackDuckVersionName = versionName;
        }
        return blackDuckVersionName;
    }
}
