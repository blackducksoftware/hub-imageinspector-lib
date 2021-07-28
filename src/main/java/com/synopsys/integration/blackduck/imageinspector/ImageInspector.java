/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.api.*;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystem;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemCompatibilityChecker;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.PkgMgrDbExtractor;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.image.common.*;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentHierarchyBuilder;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchyLogger;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.TarOperations;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;

@Component
public class ImageInspector {
    private static final String NO_PKG_MGR_FOUND = "noPkgMgr";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Os os;
    private final TarOperations tarOperations;
    private final FileOperations fileOperations;
    private final PkgMgrDbExtractor pkgMgrDbExtractor;
    private final ImageLayerApplier imageLayerApplier;
    private final ContainerFileSystemCompatibilityChecker containerFileSystemCompatibilityChecker;
    private final BdioGenerator bdioGenerator;
    private final ImageDirectoryDataExtractorFactoryChooser imageDirectoryDataExtractorFactoryChooser;
    private final ImageComponentHierarchyLogger imageComponentHierarchyLogger;

    public ImageInspector(Os os, PkgMgrDbExtractor pkgMgrDbExtractor, TarOperations tarOperations,
                          FileOperations fileOperations,
                          ImageLayerApplier imageLayerApplier,
                          ContainerFileSystemCompatibilityChecker containerFileSystemCompatibilityChecker,
                          BdioGenerator bdioGenerator,
                          ImageDirectoryDataExtractorFactoryChooser imageDirectoryDataExtractorFactoryChooser,
                          ImageComponentHierarchyLogger imageComponentHierarchyLogger) {
        this.os = os;
        this.pkgMgrDbExtractor = pkgMgrDbExtractor;
        this.tarOperations = tarOperations;
        this.fileOperations = fileOperations;
        this.imageLayerApplier = imageLayerApplier;
        this.containerFileSystemCompatibilityChecker = containerFileSystemCompatibilityChecker;
        this.bdioGenerator = bdioGenerator;
        this.imageDirectoryDataExtractorFactoryChooser = imageDirectoryDataExtractorFactoryChooser;
        this.imageComponentHierarchyLogger = imageComponentHierarchyLogger;
    }

    public ImageInfoDerived inspectImage(List<ImageDirectoryDataExtractorFactory> imageDirectoryDataExtractorFactories, ComponentHierarchyBuilder componentHierarchyBuilder, final ImageInspectionRequest imageInspectionRequest,
                                         final File workingDir,
                                         final String effectivePlatformTopLayerExternalId)
            throws IOException, IntegrationException {

        final File targetImageTarfile = new File(imageInspectionRequest.getDockerTarfilePath());
        WorkingDirectories workingDirectories = new WorkingDirectories(workingDir);
        File imageDir = workingDirectories.getExtractedImageDir(targetImageTarfile);
        tarOperations.extractTarToGivenDir(imageDir, targetImageTarfile);

        ImageDirectoryDataExtractorFactory imageDirectoryDataExtractorFactory = imageDirectoryDataExtractorFactoryChooser.choose(imageDirectoryDataExtractorFactories, imageDir);
        ImageDirectoryDataExtractor imageDirectoryDataExtractor = imageDirectoryDataExtractorFactory.createImageDirectoryDataExtractor();
        ImageLayerMetadataExtractor imageLayerMetadataExtractor = imageDirectoryDataExtractorFactory.createImageLayerMetadataExtractor();

        ImageDirectoryData imageDirectoryData = imageDirectoryDataExtractor.extract(imageDir, imageInspectionRequest.getGivenImageRepo(), imageInspectionRequest.getGivenImageTag());
        File targetImageFileSystemRootDir = workingDirectories.getTargetImageFileSystemRootDir(imageDirectoryData.getActualRepo(), imageDirectoryData.getActualTag());
        File targetImageFileSystemAppLayersRootDir = null;
        if (StringUtils.isNotBlank(effectivePlatformTopLayerExternalId)) {
            targetImageFileSystemAppLayersRootDir = workingDirectories.getTargetImageFileSystemAppLayersRootDir(imageDirectoryData.getActualRepo(), imageDirectoryData.getActualTag());
        }
        final ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir, targetImageFileSystemAppLayersRootDir);
        final ImageInspectorOsEnum currentOs = os.deriveOs(imageInspectionRequest.getCurrentLinuxDistro());
        final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = applyImageLayersToContainerFileSystem(imageLayerMetadataExtractor,
                imageInspectionRequest.getTargetLinuxDistroOverride(), containerFileSystem, imageDirectoryData.getOrderedLayerArchives(), imageDirectoryData.getFullLayerMapping(),
                        imageInspectionRequest.getPlatformTopLayerExternalId(), componentHierarchyBuilder);
        containerFileSystemCompatibilityChecker.checkInspectorOs(containerFileSystemWithPkgMgrDb, currentOs);
        ImageComponentHierarchy imageComponentHierarchy = componentHierarchyBuilder.build();
        verifyPlatformTopLayerFound(effectivePlatformTopLayerExternalId, imageComponentHierarchy);
        imageComponentHierarchyLogger.log(imageComponentHierarchy);
        cleanUpLayerTars(imageInspectionRequest.isCleanupWorkingDir(), imageDirectoryData.getOrderedLayerArchives());
        ImageInfoDerived imageInfoDerived = generateBdioFromGivenComponents(bdioGenerator, containerFileSystemWithPkgMgrDb, imageDirectoryData.getFullLayerMapping(),
                imageComponentHierarchy,
                imageInspectionRequest.getBlackDuckProjectName(), imageInspectionRequest.getBlackDuckProjectVersion(),
                imageInspectionRequest.getCodeLocationPrefix(), imageInspectionRequest.isOrganizeComponentsByLayer(), imageInspectionRequest.isIncludeRemovedComponents(),
                StringUtils.isNotBlank(effectivePlatformTopLayerExternalId));
        createContainerFileSystemTarIfRequested(containerFileSystem, imageInspectionRequest.getContainerFileSystemOutputPath(),
                imageInspectionRequest.getContainerFileSystemExcludedPathListString());
        return imageInfoDerived;
    }

    private void verifyPlatformTopLayerFound(final String givenPlatformTopLayerExternalId, final ImageComponentHierarchy imageComponentHierarchy) throws IntegrationException {
        if ((StringUtils.isNotBlank(givenPlatformTopLayerExternalId)) && (!imageComponentHierarchy.isPlatformTopLayerFound())) {
            throw new IntegrationException(String.format("Platform top layer id (%s) was specified but not found", givenPlatformTopLayerExternalId));
        }
    }

    private void cleanUpLayerTars(final boolean cleanupWorkingDir, final List<TypedArchiveFile> layerTars) {
        if (cleanupWorkingDir) {
            for (final TypedArchiveFile layerTar : layerTars) {
                logger.trace(String.format("Deleting %s", layerTar.getFile().getAbsolutePath()));
                fileOperations.deleteQuietly(layerTar.getFile());
            }
        }
    }

    private void createContainerFileSystemTarIfRequested(final ContainerFileSystem containerFileSystem, final String containerFileSystemOutputPath, final String containerFileSystemExcludedPathListString) {
        if (StringUtils.isNotBlank(containerFileSystemOutputPath)) {
            logger.info("Including container file system in output");
            final File outputDirectory = new File(containerFileSystemOutputPath);
            final File containerFileSystemTarFile = new File(containerFileSystemOutputPath);
            final File returnedTargetImageFileSystem = containerFileSystem.getTargetImageFileSystemAppOnly().orElse(containerFileSystem.getTargetImageFileSystemFull());
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), returnedTargetImageFileSystem.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final LinuxFileSystem containerFileSys = new LinuxFileSystem(returnedTargetImageFileSystem, fileOperations);
            containerFileSys.writeToTarGz(containerFileSystemTarFile, containerFileSystemExcludedPathListString);
        }
    }

    private ContainerFileSystemWithPkgMgrDb applyImageLayersToContainerFileSystem(ImageLayerMetadataExtractor imageLayerMetadataExtractor,
                                                                                  final String targetLinuxDistroOverride, final ContainerFileSystem containerFileSystem, final List<TypedArchiveFile> orderedLayerTars,
                                                                                  final FullLayerMapping layerMapping, final String platformTopLayerExternalId,
                                                                                  ComponentHierarchyBuilder componentHierarchyBuilder) throws IOException, WrongInspectorOsException {

        Optional<Integer> platformTopLayerIndex = layerMapping.getPlatformTopLayerIndex(platformTopLayerExternalId);
        if (platformTopLayerIndex.isPresent()) {
            componentHierarchyBuilder.setPlatformTopLayerIndex(platformTopLayerIndex.get());
        }

        ContainerFileSystemWithPkgMgrDb postLayerContainerFileSystemWithPkgMgrDb = null;
        boolean inApplicationLayers = false;
        int layerIndex = 0;
        for (TypedArchiveFile layerTar : orderedLayerTars) {
            imageLayerApplier.applyLayer(containerFileSystem.getTargetImageFileSystemFull(), layerTar);
            if (inApplicationLayers && containerFileSystem.getTargetImageFileSystemAppOnly().isPresent()) {
                imageLayerApplier.applyLayer(containerFileSystem.getTargetImageFileSystemAppOnly().get(), layerTar);
            }
            LayerMetadata layerMetadata = imageLayerMetadataExtractor.getLayerMetadata(layerMapping, layerTar, layerIndex);
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
        }
        return postLayerContainerFileSystemWithPkgMgrDb;
    }

    ImageInfoDerived generateBdioFromGivenComponents(final BdioGenerator bdioGenerator, ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, final FullLayerMapping fullLayerMapping,
                                                            ImageComponentHierarchy imageComponentHierarchy,
                                                            final String givenProjectName,
                                                            final String givenProjectVersionName,
                                                            final String codeLocationPrefix,
                                                            final boolean organizeComponentsByLayer,
                                                            final boolean includeRemovedComponents,
                                                            final boolean platformComponentsExcluded) {
        String codeLocationName = deriveCodeLocationName(codeLocationPrefix, containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase(),
                fullLayerMapping.getManifestLayerMapping().getImageName(), fullLayerMapping.getManifestLayerMapping().getTagName(), platformComponentsExcluded);
        String finalProjectName = deriveBlackDuckProject(fullLayerMapping.getManifestLayerMapping().getImageName(), givenProjectName, platformComponentsExcluded);
        String finalProjectVersionName = deriveBlackDuckProjectVersion(fullLayerMapping, givenProjectVersionName);
        final SimpleBdioDocument bdioDocument = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy(codeLocationName,
                finalProjectName, finalProjectVersionName, containerFileSystemWithPkgMgrDb.getLinuxDistroName(), imageComponentHierarchy, organizeComponentsByLayer,
                includeRemovedComponents);
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(fullLayerMapping, finalProjectName, finalProjectVersionName, codeLocationName,
                containerFileSystemWithPkgMgrDb, imageComponentHierarchy, platformComponentsExcluded, bdioDocument);
        return imageInfoDerived;
    }

    private ImageInfoDerived deriveImageInfo(final FullLayerMapping fullLayerMapping, final String finalProjectName, final String finalProjectVersionName,
                                             final String codeLocationName, final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, ImageComponentHierarchy imageComponentHierarchy,
                                             final boolean platformComponentsExcluded, SimpleBdioDocument bdioDocument) {
        logger.debug(String.format("deriveImageInfo(): projectName: %s, versionName: %s", finalProjectName, finalProjectVersionName));
        final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(fullLayerMapping, containerFileSystemWithPkgMgrDb, imageComponentHierarchy,
                codeLocationName,
                finalProjectName, finalProjectVersionName, bdioDocument);
        logger.info(String.format("Black Duck project: %s, version: %s; Code location : %s", imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getCodeLocationName()));
        return imageInfoDerived;
    }

    private String deriveCodeLocationName(final String codeLocationPrefix, ImagePkgMgrDatabase imagePkgMgrDatabase, String repo, String tag, final boolean platformComponentsExcluded) {
        final String pkgMgrName = derivePackageManagerName(imagePkgMgrDatabase);
        return Names.getCodeLocationName(codeLocationPrefix, repo, tag,
                pkgMgrName, platformComponentsExcluded);
    }

    private String derivePackageManagerName(ImagePkgMgrDatabase imagePkgMgrDatabase) {
        final String pkgMgrName;
        if (imagePkgMgrDatabase != null && imagePkgMgrDatabase.getPackageManager() != PackageManagerEnum.NULL) {
            pkgMgrName = imagePkgMgrDatabase.getPackageManager().toString();
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
