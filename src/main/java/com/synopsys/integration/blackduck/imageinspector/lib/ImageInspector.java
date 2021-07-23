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

import com.synopsys.integration.blackduck.imageinspector.api.*;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.*;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerImageDirectoryAnalyzer;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerImageLayerArchiveAnalyzer;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.TarOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;

// As support for other image formats is added, this class will manage the list of TarParsers
@Component
public class ImageInspector {
    public static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    public static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private static final String NO_PKG_MGR_FOUND = "noPkgMgr";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Os os;
    private final TarOperations tarOperations;
    private final GsonBuilder gsonBuilder;
    private final FileOperations fileOperations;
    private final DockerImageConfigParser dockerImageConfigParser;
    private final DockerManifestFactory dockerManifestFactory;
    private final List<PkgMgr> pkgMgrs;
    private final PkgMgrDbExtractor pkgMgrDbExtractor;
    private final ImageLayerApplier imageLayerApplier;
    private final DockerImageLayerArchiveAnalyzer dockerImageLayerArchive;
    private final ContainerFileSystemAnalyzer containerFileSystemAnalyzer;
    private final BdioGenerator bdioGenerator;

    public ImageInspector(Os os, List<PkgMgr> pkgMgrs, PkgMgrDbExtractor pkgMgrDbExtractor, TarOperations tarOperations, GsonBuilder gsonBuilder,
                          FileOperations fileOperations, DockerImageConfigParser dockerImageConfigParser, DockerManifestFactory dockerManifestFactory,
                          ImageLayerApplier imageLayerApplier, DockerImageLayerArchiveAnalyzer dockerImageLayerArchive,
                          ContainerFileSystemAnalyzer containerFileSystemAnalyzer,
                          BdioGenerator bdioGenerator) {
        this.os = os;
        this.pkgMgrs = pkgMgrs;
        this.pkgMgrDbExtractor = pkgMgrDbExtractor;
        this.tarOperations = tarOperations;
        this.gsonBuilder = gsonBuilder;
        this.fileOperations = fileOperations;
        this.dockerImageConfigParser = dockerImageConfigParser;
        this.dockerManifestFactory = dockerManifestFactory;
        this.imageLayerApplier = imageLayerApplier;
        this.dockerImageLayerArchive = dockerImageLayerArchive;
        this.containerFileSystemAnalyzer = containerFileSystemAnalyzer;
        this.bdioGenerator = bdioGenerator;
    }

    ///////////////////////////////////////
    public ImageInfoDerived inspectUsingGivenWorkingDir(ComponentHierarchyBuilder componentHierarchyBuilder, final ImageInspectionRequest imageInspectionRequest,
                                                         final File tempDir,
                                                         final String effectivePlatformTopLayerExternalId)
            throws IOException, IntegrationException {

        final File workingDir = new File(tempDir, "working");
        final File tarExtractionBaseDirectory = getTarExtractionDirectory(workingDir);
        logger.debug(String.format("workingDir: %s", workingDir.getAbsolutePath()));
        final File dockerTarfile = new File(imageInspectionRequest.getDockerTarfilePath());
        File imageDir = new File(tarExtractionBaseDirectory, dockerTarfile.getName());
        final File dockerImageDirectory = extractImageTar(imageDir, dockerTarfile);

        ////////////////
        // TODO This (and anything that is Docker-specific) is too low-level for this class
        // TODO: inject this or a factory or s.t.
        // TODO use Abstract Factory Pattern to create Docker or OCI-specific objects? Has to be driven by the actual image passed in
        GsonBuilder gsonBuilder = new GsonBuilder();
        FileOperations fileOperations = new FileOperations();
        DockerImageConfigParser dockerImageConfigParser = new DockerImageConfigParser();
        DockerManifestFactory dockerManifestFactory = new DockerManifestFactory();
        ImageDirectoryAnalyzer imageDirectoryAnalyzer = new DockerImageDirectoryAnalyzer(gsonBuilder, fileOperations, dockerImageConfigParser,
                dockerManifestFactory);
        //////////////
        final List<TypedArchiveFile> unOrderedLayerTars = imageDirectoryAnalyzer.getLayerArchives(imageDir);
        final FullLayerMapping fullLayerMapping = imageDirectoryAnalyzer.getLayerMapping(imageDir, imageInspectionRequest.getGivenImageRepo(), imageInspectionRequest.getGivenImageTag());
        final String imageRepo = fullLayerMapping.getManifestLayerMapping().getImageName();
        final String imageTag = fullLayerMapping.getManifestLayerMapping().getTagName();

        final File targetImageFileSystemParentDir = new File(tarExtractionBaseDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        File targetImageFileSystemAppLayersRootDir = null;
        if (StringUtils.isNotBlank(effectivePlatformTopLayerExternalId)) {
            targetImageFileSystemAppLayersRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemAppLayersRootDirName(imageRepo, imageTag));
        }
        final ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir, targetImageFileSystemAppLayersRootDir);
        // TODO this seems like a pretty low level operation for this class
        final ImageInspectorOsEnum currentOs = os.deriveOs(imageInspectionRequest.getCurrentLinuxDistro());

        ImageLayerArchives imageLayerArchives = new ImageLayerArchives(unOrderedLayerTars, fullLayerMapping.getManifestLayerMapping());
        List<TypedArchiveFile> orderedLayerArchives = imageLayerArchives.getOrderedLayerArchives();
        final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = extractDockerLayers(currentOs,
                imageInspectionRequest.getTargetLinuxDistroOverride(), containerFileSystem, orderedLayerArchives, fullLayerMapping,
                        imageInspectionRequest.getPlatformTopLayerExternalId(), componentHierarchyBuilder);
        ImageComponentHierarchy imageComponentHierarchy = componentHierarchyBuilder.build();
        validatePlatformResults(effectivePlatformTopLayerExternalId, imageComponentHierarchy);
        logLayers(imageComponentHierarchy);
        cleanUpLayerTars(imageInspectionRequest.isCleanupWorkingDir(), unOrderedLayerTars);
        ImageInfoDerived imageInfoDerived = generateBdioFromGivenComponents(bdioGenerator, containerFileSystemWithPkgMgrDb, fullLayerMapping,
                imageComponentHierarchy,
                imageInspectionRequest.getBlackDuckProjectName(), imageInspectionRequest.getBlackDuckProjectVersion(),
                imageInspectionRequest.getCodeLocationPrefix(), imageInspectionRequest.isOrganizeComponentsByLayer(), imageInspectionRequest.isIncludeRemovedComponents(),
                StringUtils.isNotBlank(effectivePlatformTopLayerExternalId));
        createContainerFileSystemTarIfRequested(containerFileSystem, imageInspectionRequest.getContainerFileSystemOutputPath(),
                imageInspectionRequest.getContainerFileSystemExcludedPathListString());
        return imageInfoDerived;
    }

    private void validatePlatformResults(final String givenPlatformTopLayerExternalId, final ImageComponentHierarchy imageComponentHierarchy) throws IntegrationException {
        if ((StringUtils.isNotBlank(givenPlatformTopLayerExternalId)) && (!imageComponentHierarchy.isPlatformTopLayerFound())) {
            throw new IntegrationException(String.format("Platform top layer id (%s) was specified but not found", givenPlatformTopLayerExternalId));
        }
    }

    private void logLayers(final ImageComponentHierarchy imageComponentHierarchy) {
        if (!logger.isTraceEnabled()) {
            return;
        }
        logger.trace("layer dump:");
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            if (layer == null) {
                logger.trace("Layer is null");
            } else if (layer.getComponents() == null) {
                logger.trace(String.format("layer %s has no componenents", layer.getLayerIndexedName()));
            } else {
                logger.trace(String.format("Layer %s has %d components; layer cmd: %s", layer.getLayerIndexedName(), layer.getComponents().size(), layer.getLayerCmd()));
            }
        }
        if (imageComponentHierarchy.getFinalComponents() == null) {
            logger.trace("Final image components list not set");
        } else {
            logger.trace(String.format("Final image components list has %d components", imageComponentHierarchy.getFinalComponents().size()));
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

    ///////////////////////////////////////

    File getTarExtractionDirectory(final File workingDirectory) {
        return new File(workingDirectory, TAR_EXTRACTION_DIRECTORY);
    }

    File extractImageTar(final File tarExtractionDirectory, final File dockerTar) throws IOException {
        return tarOperations.extractTarToGivenDir(tarExtractionDirectory, dockerTar);
    }

    private ContainerFileSystemWithPkgMgrDb extractDockerLayers(final ImageInspectorOsEnum currentOs, final String targetLinuxDistroOverride, final ContainerFileSystem containerFileSystem, final List<TypedArchiveFile> orderedLayerTars,
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
            imageLayerApplier.extractLayerTar(containerFileSystem.getTargetImageFileSystemFull(), layerTar);
            if (inApplicationLayers && containerFileSystem.getTargetImageFileSystemAppOnly().isPresent()) {
                imageLayerApplier.extractLayerTar(containerFileSystem.getTargetImageFileSystemAppOnly().get(), layerTar);
            }
            LayerMetadata layerMetadata = dockerImageLayerArchive.getLayerMetadata(layerMapping, layerTar, layerIndex);
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
            containerFileSystemAnalyzer.checkInspectorOs(postLayerContainerFileSystemWithPkgMgrDb, currentOs);
        }
        // TODO This has always returned null in some cases, like a scratch image or a windows image, so should be OK w/out a null check
        return postLayerContainerFileSystemWithPkgMgrDb;
        //OLD: return tarParser.extractPkgMgrDb(currentOs, targetLinuxDistroOverride, containerFileSystem, unOrderedLayerTars, layerMapping, platformTopLayerExternalId);
    }

    ImageInfoDerived generateBdioFromGivenComponents(final BdioGenerator bdioGenerator, ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, final FullLayerMapping mapping,
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
