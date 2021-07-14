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

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerImageDirectory;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.TarOperations;
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
import com.synopsys.integration.exception.IntegrationException;

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

    public ImageInspector(final DockerTarParser tarParser, TarOperations tarOperations, GsonBuilder gsonBuilder,
                          FileOperations fileOperations, DockerImageConfigParser dockerImageConfigParser, DockerManifestFactory dockerManifestFactory) {
        this.tarParser = tarParser;
        this.tarOperations = tarOperations;
        this.gsonBuilder = gsonBuilder;
        this.fileOperations = fileOperations;
        this.dockerImageConfigParser = dockerImageConfigParser;
        this.dockerManifestFactory = dockerManifestFactory;
    }

    public File getTarExtractionDirectory(final File workingDirectory) {
        return new File(workingDirectory, TAR_EXTRACTION_DIRECTORY);
    }

    public DockerImageDirectory extractImageTar(final File tarExtractionDirectory, final File dockerTar) throws IOException {
        File imageDir = tarOperations.extractTarToGivenDir(tarExtractionDirectory, dockerTar);
        return new DockerImageDirectory(gsonBuilder, fileOperations, dockerImageConfigParser, dockerManifestFactory, imageDir);
    }

    public ImageInfoParsed extractDockerLayers(final ImageInspectorOsEnum currentOs, final String targetLinuxDistroOverride, final TargetImageFileSystem targetImageFileSystem, final List<TypedArchiveFile> layerTars,
        final ManifestLayerMapping layerMapping, final String platformTopLayerExternalId) throws IOException, WrongInspectorOsException {
        return tarParser.extractImageLayers(currentOs, targetLinuxDistroOverride, targetImageFileSystem, layerTars, layerMapping, platformTopLayerExternalId);
    }

    public ImageInfoDerived generateBdioFromGivenComponents(final BdioGenerator bdioGenerator, ImageInfoParsed imageInfoParsed, final ManifestLayerMapping mapping,
        final String projectName,
        final String versionName,
        final String codeLocationPrefix,
        final boolean organizeComponentsByLayer,
        final boolean includeRemovedComponents,
        final boolean platformComponentsExcluded) {
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(mapping, projectName, versionName, codeLocationPrefix, imageInfoParsed, platformComponentsExcluded);
        final SimpleBdioDocument bdioDocument = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy(imageInfoDerived.getCodeLocationName(),
            imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getImageInfoParsed().getLinuxDistroName(), imageInfoParsed.getImageComponentHierarchy(), organizeComponentsByLayer,
            includeRemovedComponents);
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    private ImageInfoDerived deriveImageInfo(final ManifestLayerMapping mapping, final String projectName, final String versionName,
        final String codeLocationPrefix, final ImageInfoParsed imageInfoParsed, final boolean platformComponentsExcluded) {
        logger.debug(String.format("deriveImageInfo(): projectName: %s, versionName: %s", projectName, versionName));
        final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(imageInfoParsed);
        imageInfoDerived.setManifestLayerMapping(mapping);
        imageInfoDerived.setCodeLocationName(deriveCodeLocationName(codeLocationPrefix, imageInfoDerived, platformComponentsExcluded));
        imageInfoDerived.setFinalProjectName(deriveBlackDuckProject(imageInfoDerived.getManifestLayerMapping().getImageName(), projectName, platformComponentsExcluded));
        imageInfoDerived.setFinalProjectVersionName(deriveBlackDuckProjectVersion(imageInfoDerived.getManifestLayerMapping(), versionName));
        logger.info(String.format("Black Duck project: %s, version: %s; Code location : %s", imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getCodeLocationName()));
        return imageInfoDerived;
    }

    private String deriveCodeLocationName(final String codeLocationPrefix, final ImageInfoDerived imageInfoDerived, final boolean platformComponentsExcluded) {
        final String pkgMgrName = derivePackageManagerName(imageInfoDerived);
        return Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
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

    private String deriveBlackDuckProjectVersion(final ManifestLayerMapping mapping, final String versionName) {
        String blackDuckVersionName;
        if (StringUtils.isBlank(versionName)) {
            blackDuckVersionName = mapping.getTagName();
        } else {
            logger.debug("Using project version from config property");
            blackDuckVersionName = versionName;
        }
        return blackDuckVersionName;
    }
}
