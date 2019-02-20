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
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.linux.extraction.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extraction.ComponentExtractorFactory;
import com.synopsys.integration.exception.IntegrationException;

// As support for other image formats is added, this class will manage the list of TarParsers
@Component
public class ImageInspector {
    public static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    public static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private static final String NO_PKG_MGR_FOUND = "noPkgMgr";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DockerTarParser tarParser;
    private final ComponentExtractorFactory componentExtractorFactory;

    public ImageInspector(final DockerTarParser tarParser, final ComponentExtractorFactory componentExtractorFactory) {
        this.tarParser = tarParser;
        this.componentExtractorFactory = componentExtractorFactory;
    }

    public File getTarExtractionDirectory(final File workingDirectory) {
        return new File(workingDirectory, TAR_EXTRACTION_DIRECTORY);
    }

    public List<File> extractLayerTars(final File tarExtractionDirectory, final File dockerTar) throws IOException {
        return tarParser.extractLayerTars(tarExtractionDirectory, dockerTar);
    }

    public ImageInfoParsed extractDockerLayers(final ImageInspectorOsEnum currentOs, final ImageComponentHierarchy imageComponentHierarchy, final File containerFileSystemRootDir, final List<File> layerTars,
        final ManifestLayerMapping layerMapping) throws IOException,
                                                            WrongInspectorOsException {
        return tarParser.extractDockerLayers(componentExtractorFactory, currentOs, imageComponentHierarchy, containerFileSystemRootDir, layerTars, layerMapping);
    }

    public ManifestLayerMapping getLayerMapping(final GsonBuilder gsonBuilder, final File tarExtractionDirectory, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        return tarParser.getLayerMapping(gsonBuilder, tarExtractionDirectory, tarFileName, dockerImageName, dockerTagName);
    }

    public ImageComponentHierarchy createInitialImageComponentHierarchy(final File workingDirectory, final File tarExtractionDirectory, final String tarFileName, final ManifestLayerMapping manifestLayerMapping) throws IntegrationException {
        return tarParser.createInitialImageComponentHierarchy(tarExtractionDirectory, tarFileName, manifestLayerMapping);
    }

    public ImageInfoDerived generateBdioFromGivenComponents(final BdioGenerator bdioGenerator, ImageInfoParsed imageInfoParsed, final ImageComponentHierarchy imageComponentHierarchy, final ManifestLayerMapping mapping,
        final String projectName,
        final String versionName,
        final String codeLocationPrefix,
        final boolean organizeComponentsByLayer,
        final boolean includeRemovedComponents) {
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(mapping, projectName, versionName, codeLocationPrefix, imageInfoParsed);
        final SimpleBdioDocument bdioDocument = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy(imageInfoDerived.getCodeLocationName(),
            imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getImageInfoParsed().getLinuxDistroName(), imageComponentHierarchy, organizeComponentsByLayer, includeRemovedComponents);
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    private ImageInfoDerived deriveImageInfo(final ManifestLayerMapping mapping, final String projectName, final String versionName,
        final String codeLocationPrefix, final ImageInfoParsed imageInfoParsed) {
        logger.debug(String.format("deriveImageInfo(): projectName: %s, versionName: %s", projectName, versionName));
        final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(imageInfoParsed);
        imageInfoDerived.setManifestLayerMapping(mapping);
        imageInfoDerived.setCodeLocationName(deriveCodeLocationName(codeLocationPrefix, imageInfoDerived));
        imageInfoDerived.setFinalProjectName(deriveBlackDuckProject(imageInfoDerived.getManifestLayerMapping().getImageName(), projectName));
        imageInfoDerived.setFinalProjectVersionName(deriveBlackDuckProjectVersion(imageInfoDerived.getManifestLayerMapping(), versionName));
        logger.info(String.format("Black Duck project: %s, version: %s; Code location : %s", imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getCodeLocationName()));
        return imageInfoDerived;
    }

    private String deriveCodeLocationName(final String codeLocationPrefix, final ImageInfoDerived imageInfoDerived) {
        final String pkgMgrName = derivePackageManagerName(imageInfoDerived);
        return Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                pkgMgrName);
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

    private String deriveBlackDuckProject(final String imageName, final String projectName) {
        String blackDuckProjectName;
        if (StringUtils.isBlank(projectName)) {
            blackDuckProjectName = Names.getBlackDuckProjectNameFromImageName(imageName);
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
