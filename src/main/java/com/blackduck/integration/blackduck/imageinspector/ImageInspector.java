/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.blackduck.integration.blackduck.imageinspector.api.*;
import com.blackduck.integration.blackduck.imageinspector.image.common.*;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystem;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemCompatibilityChecker;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.PkgMgrDbExtractor;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentHierarchyBuilder;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchy;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchyLogger;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.blackduck.integration.blackduck.imageinspector.linux.Os;
import com.blackduck.integration.blackduck.imageinspector.linux.TarOperations;
import com.blackduck.integration.exception.IntegrationException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackduck.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.blackduck.imageinspector.api.name.Names;
import com.blackduck.integration.blackduck.imageinspector.bdio.BdioGenerator;

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

        final File targetImageTarfile = new File(imageInspectionRequest.getImageTarfilePath());
        WorkingDirectories workingDirectories = new WorkingDirectories(workingDir);
        tarOperations.extractTarToGivenDir(workingDirectories.getExtractedImageDir(targetImageTarfile), targetImageTarfile);
        ImageDirectoryDataExtractorFactory imageDirectoryDataExtractorFactory = imageDirectoryDataExtractorFactoryChooser.choose(imageDirectoryDataExtractorFactories, workingDirectories.getExtractedImageDir(targetImageTarfile));
        ImageDirectoryDataExtractor imageDirectoryDataExtractor = imageDirectoryDataExtractorFactory.createImageDirectoryDataExtractor();
        ImageDirectoryData imageDirectoryData = imageDirectoryDataExtractor.extract(workingDirectories.getExtractedImageDir(targetImageTarfile), imageInspectionRequest.getGivenImageRepo(), imageInspectionRequest.getGivenImageTag());
        final ContainerFileSystem containerFileSystem = prepareContainerFileSystem(effectivePlatformTopLayerExternalId, workingDirectories, imageDirectoryData);
        final ImageInspectorOsEnum currentOs = os.deriveOs(imageInspectionRequest.getCurrentLinuxDistro());
        ImageLayerMetadataExtractor imageLayerMetadataExtractor = imageDirectoryDataExtractorFactory.createImageLayerMetadataExtractor();

        Optional<Integer> platformTopLayerIndex = imageDirectoryData.getPlatformTopLayerIndex(imageInspectionRequest.getPlatformTopLayerExternalId());
        if (platformTopLayerIndex.isPresent()) {
            componentHierarchyBuilder.setPlatformTopLayerIndex(platformTopLayerIndex.get());
        }

        final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = applyImageLayersToContainerFileSystem(imageLayerMetadataExtractor,
                imageInspectionRequest.getTargetLinuxDistroOverride(), containerFileSystem, componentHierarchyBuilder, imageDirectoryData.getLayerData(), imageDirectoryData.getFullLayerMapping());

        containerFileSystemCompatibilityChecker.checkInspectorOs(containerFileSystemWithPkgMgrDb, currentOs);
        ImageComponentHierarchy imageComponentHierarchy = componentHierarchyBuilder.build();
        verifyPlatformTopLayerFound(effectivePlatformTopLayerExternalId, imageComponentHierarchy);
        imageComponentHierarchyLogger.log(imageComponentHierarchy);
        cleanUpLayerTars(imageInspectionRequest.isCleanupWorkingDir(), imageDirectoryData.getLayerData());
        ImageInfoDerived imageInfoDerived = generateBdioFromGivenComponents(bdioGenerator, containerFileSystemWithPkgMgrDb, imageDirectoryData.getFullLayerMapping(),
                imageComponentHierarchy,
                imageInspectionRequest.getBlackDuckProjectName(), imageInspectionRequest.getBlackDuckProjectVersion(),
                targetImageTarfile.getName(),
                imageInspectionRequest.getCodeLocationPrefix(), imageInspectionRequest.isOrganizeComponentsByLayer(), imageInspectionRequest.isIncludeRemovedComponents(),
                StringUtils.isNotBlank(effectivePlatformTopLayerExternalId));
        createContainerFileSystemTarIfRequested(containerFileSystem, imageInspectionRequest.getContainerFileSystemOutputPath(),
                imageInspectionRequest.getContainerFileSystemExcludedPathListString());
        return imageInfoDerived;
    }

    @NotNull
    private ContainerFileSystem prepareContainerFileSystem(String effectivePlatformTopLayerExternalId, WorkingDirectories workingDirectories, ImageDirectoryData imageDirectoryData) {
        File targetImageFileSystemRootDir = workingDirectories.getTargetImageFileSystemRootDir(imageDirectoryData.getActualRepo(), imageDirectoryData.getActualTag());
        File targetImageFileSystemAppLayersRootDir = null;
        if (StringUtils.isNotBlank(effectivePlatformTopLayerExternalId)) {
            targetImageFileSystemAppLayersRootDir = workingDirectories.getTargetImageFileSystemAppLayersRootDir(imageDirectoryData.getActualRepo(), imageDirectoryData.getActualTag());
        }
        final ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir, targetImageFileSystemAppLayersRootDir);
        return containerFileSystem;
    }

    private void verifyPlatformTopLayerFound(final String givenPlatformTopLayerExternalId, final ImageComponentHierarchy imageComponentHierarchy) throws IntegrationException {
        if ((StringUtils.isNotBlank(givenPlatformTopLayerExternalId)) && (!imageComponentHierarchy.isPlatformTopLayerFound())) {
            throw new IntegrationException(String.format("Platform top layer id (%s) was specified but not found", givenPlatformTopLayerExternalId));
        }
    }

    private void cleanUpLayerTars(final boolean cleanupWorkingDir, final List<LayerDetailsBuilder> layers) {
        if (cleanupWorkingDir) {
            for (LayerDetailsBuilder layer : layers) {
                File layerArchive = layer.getArchive().getFile();
                logger.trace(String.format("Deleting %s", layerArchive.getAbsolutePath()));
                fileOperations.deleteQuietly(layerArchive);
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
                                                                                  final String targetLinuxDistroOverride, final ContainerFileSystem containerFileSystem,
                                                                                  ComponentHierarchyBuilder componentHierarchyBuilder, List<LayerDetailsBuilder> layerDataList, FullLayerMapping fullLayerMapping) throws IOException, WrongInspectorOsException {


        ContainerFileSystemWithPkgMgrDb postLayerContainerFileSystemWithPkgMgrDb = null;
        boolean inApplicationLayers = false;
        Optional<Integer> platformTopLayerIndex = componentHierarchyBuilder.getPlatformTopLayerIndex();
        for (LayerDetailsBuilder layerData : layerDataList) {
            postLayerContainerFileSystemWithPkgMgrDb = applyLayer(imageLayerMetadataExtractor, targetLinuxDistroOverride, containerFileSystem, componentHierarchyBuilder, postLayerContainerFileSystemWithPkgMgrDb, inApplicationLayers, layerData, fullLayerMapping);
            if (platformTopLayerIndex.isPresent() && (layerData.getLayerIndex() == platformTopLayerIndex.get())) {
                inApplicationLayers = true; // for subsequent iterations
            }
        }
        // Never did find a pkg mgr, so create the result without one
        if (postLayerContainerFileSystemWithPkgMgrDb == null) {
            postLayerContainerFileSystemWithPkgMgrDb = new ContainerFileSystemWithPkgMgrDb(containerFileSystem, new ImagePkgMgrDatabase(null, PackageManagerEnum.NULL), targetLinuxDistroOverride, null);
        }
        return postLayerContainerFileSystemWithPkgMgrDb;
    }

    private ContainerFileSystemWithPkgMgrDb applyLayer(ImageLayerMetadataExtractor imageLayerMetadataExtractor, String targetLinuxDistroOverride, ContainerFileSystem containerFileSystem, ComponentHierarchyBuilder componentHierarchyBuilder, ContainerFileSystemWithPkgMgrDb postLayerContainerFileSystemWithPkgMgrDb, boolean inApplicationLayers, LayerDetailsBuilder layerData, FullLayerMapping fullLayerMapping) throws IOException, WrongInspectorOsException {
        imageLayerApplier.applyLayer(containerFileSystem.getTargetImageFileSystemFull(), layerData.getArchive());
        if (inApplicationLayers && containerFileSystem.getTargetImageFileSystemAppOnly().isPresent()) {
            // We're building two filesystems: 1 where we have all the files (guaranteed pkg mgr db included), and 1 with just files associated with the actual application (for ppl that don't car about base image)
            imageLayerApplier.applyLayer(containerFileSystem.getTargetImageFileSystemAppOnly().get(), layerData.getArchive());
        }
        LayerMetadata layerMetadata = imageLayerMetadataExtractor.getLayerMetadata(fullLayerMapping, layerData);
        layerData.setCmd(layerMetadata.getLayerCmd());
        try {
            postLayerContainerFileSystemWithPkgMgrDb = pkgMgrDbExtractor.extract(containerFileSystem, targetLinuxDistroOverride);
            componentHierarchyBuilder.addLayer(postLayerContainerFileSystemWithPkgMgrDb, layerData);
        } catch (PkgMgrDataNotFoundException pkgMgrDataNotFoundException) {
            logger.debug(String.format("Unable to collect components present after layer %d: The file system is not yet populated with the linux distro and package manager files: %s", layerData.getLayerIndex(), pkgMgrDataNotFoundException.getMessage()));
        }
        return postLayerContainerFileSystemWithPkgMgrDb;
    }

    ImageInfoDerived generateBdioFromGivenComponents(final BdioGenerator bdioGenerator, ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, final FullLayerMapping fullLayerMapping,
                                                            ImageComponentHierarchy imageComponentHierarchy,
                                                            final String givenProjectName,
                                                            final String givenProjectVersionName,
                                                            final String givenArchiveFilename,
                                                            final String codeLocationPrefix,
                                                            final boolean organizeComponentsByLayer,
                                                            final boolean includeRemovedComponents,
                                                            final boolean platformComponentsExcluded) {
        //TODO- what to do when image repo/tag aren't known (oci image directory does not contain that info)?
        String codeLocationName = deriveCodeLocationName(codeLocationPrefix, containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase(),
                fullLayerMapping.getManifestLayerMapping().getImageName().orElse(null), fullLayerMapping.getManifestLayerMapping().getTagName().orElse(null), givenArchiveFilename, platformComponentsExcluded);
        String finalProjectName = deriveBlackDuckProject(fullLayerMapping.getManifestLayerMapping().getImageName().orElse(null), givenProjectName, givenArchiveFilename, platformComponentsExcluded);
        String finalProjectVersionName = deriveBlackDuckProjectVersion(fullLayerMapping.getManifestLayerMapping().getTagName().orElse(null), givenProjectVersionName);
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

    private String deriveCodeLocationName(final String codeLocationPrefix, ImagePkgMgrDatabase imagePkgMgrDatabase, @Nullable String repo, @Nullable String tag, @Nullable String tarchiveFilename, final boolean platformComponentsExcluded) {
        final String pkgMgrName = derivePackageManagerName(imagePkgMgrDatabase);
        return Names.getCodeLocationName(codeLocationPrefix, repo, tag, tarchiveFilename,
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

    private String deriveBlackDuckProject(@Nullable String imageName, final String givenProjectName, @Nullable String archiveFilename,
        final boolean platformComponentsExcluded) {
        String blackDuckProjectName;
        if (StringUtils.isBlank(givenProjectName)) {
            blackDuckProjectName = Names.getBlackDuckProjectNameFromImageName(imageName, archiveFilename, platformComponentsExcluded);
            logger.debug("Derived project name: {}", blackDuckProjectName);
        } else {
            logger.debug("Using project from config property");
            blackDuckProjectName = givenProjectName;
        }
        return blackDuckProjectName;
    }

    private String deriveBlackDuckProjectVersion(@Nullable String givenTagName, final String givenVersionName) {
        String blackDuckVersionName;
        if (StringUtils.isBlank(givenVersionName)) {
            blackDuckVersionName = Names.getBlackDuckProjectVersionNameFromImageTag(givenTagName);
            logger.debug("Derived project version: {}", blackDuckVersionName);
        } else {
            logger.debug("Using project version from config property");
            blackDuckVersionName = givenVersionName;
        }
        return blackDuckVersionName;
    }
}
