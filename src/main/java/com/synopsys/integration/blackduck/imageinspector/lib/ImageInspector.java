/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.name.Names;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioWriter;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

@Component
public class ImageInspector {
    private static final String NO_PKG_MGR_FOUND = "noPkgMgr";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DockerTarParser tarParser;
    private ComponentExtractorFactory componentExtractorFactory;
    private final Gson gson = new Gson();

    @Autowired
    public void setComponentExtractorFactory(final ComponentExtractorFactory componentExtractorFactory) {
        this.componentExtractorFactory = componentExtractorFactory;
    }

    @Autowired
    public void setTarParser(final DockerTarParser tarParser) {
        this.tarParser = tarParser;
    }

    public File getTarExtractionDirectory(final File workingDirectory) {
        return tarParser.getTarExtractionDirectory(workingDirectory);
    }

    public List<File> extractLayerTars(final File workingDir, final File dockerTar) throws IOException {
        return tarParser.extractLayerTars(workingDir, dockerTar);
    }

    public void extractDockerLayers(final OperatingSystemEnum currentOs, final File containerFileSystemRootDir, final List<File> layerTars, final ManifestLayerMapping layerMapping) throws WrongInspectorOsException, IOException {
        tarParser.extractDockerLayers(componentExtractorFactory, currentOs, containerFileSystemRootDir, layerTars, layerMapping);
    }

    public ImageInfoParsed parseImageInfo(final File targetImageFileSystemRootDir) throws IntegrationException, IOException {
        return tarParser.parseImageInfo(targetImageFileSystemRootDir);
    }

    public ManifestLayerMapping getLayerMapping(final File workingDir, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        return tarParser.getLayerMapping(workingDir, tarFileName, dockerImageName, dockerTagName);
    }

    public ImageInfoDerived generateBdioFromImageFilesDir(final BdioGenerator bdioGenerator, ImageInfoParsed imageInfoParsed, final String dockerImageRepo, final String dockerImageTag, final ManifestLayerMapping mapping, final String projectName,
            final String versionName,
            final File targetImageFileSystemRootDir, final String codeLocationPrefix) throws IOException, IntegrationException, InterruptedException {
        // TODO will not need this null check + imageInfoParsed assignment once Exec mode is removed
        if (imageInfoParsed == null) {
            imageInfoParsed = tarParser.parseImageInfo(targetImageFileSystemRootDir);
        }
        ////
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(mapping, projectName, versionName, codeLocationPrefix, imageInfoParsed);
        final ComponentExtractor componentExtractor = componentExtractorFactory.createComponentExtractor(gson, imageInfoParsed.getFileSystemRootDir(), imageInfoParsed.getPkgMgr().getPackageManager());
        final List<ComponentDetails> comps = componentExtractor.extractComponents(imageInfoParsed.getPkgMgr(), imageInfoParsed.getLinuxDistroName());
        final SimpleBdioDocument bdioDocument = bdioGenerator.generateBdioDocument(imageInfoDerived.getCodeLocationName(),
                imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getImageInfoParsed().getLinuxDistroName(), comps);
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    public ImageInfoDerived generateEmptyBdio(final BdioGenerator bdioGenerator, final String dockerImageRepo, final String dockerImageTag, final ManifestLayerMapping mapping, final String projectName, final String versionName,
            final File targetImageFileSystemRootDir, final String codeLocationPrefix) throws IOException, IntegrationException, InterruptedException {
        final ImageInfoParsed imageInfoParsed = new ImageInfoParsed(targetImageFileSystemRootDir, null, null);
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(mapping, projectName, versionName, codeLocationPrefix, imageInfoParsed);
        final List<ComponentDetails> comps = new ArrayList<>(0);
        final SimpleBdioDocument bdioDocument = bdioGenerator.generateBdioDocument(imageInfoDerived.getCodeLocationName(), imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), null, comps);
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    public File writeBdioFile(final File outputDirectory, final ImageInfoDerived imageInfoDerived) throws IOException {
        final String bdioFilename = Names.getBdioFilename(imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getPkgMgrFilePath(), imageInfoDerived.getFinalProjectName(),
                imageInfoDerived.getFinalProjectVersionName());
        FileOperations.ensureDirExists(outputDirectory);
        final File bdioOutputFile = new File(outputDirectory, bdioFilename);
        writeBdioToFile(imageInfoDerived.getBdioDocument(), bdioOutputFile);
        return bdioOutputFile;
    }

    private ImageInfoDerived deriveImageInfo(final ManifestLayerMapping mapping, final String projectName, final String versionName,
            final String codeLocationPrefix, final ImageInfoParsed imageInfoParsed) {
        logger.debug(String.format("generateBdioFromImageFilesDir(): projectName: %s, versionName: %s", projectName, versionName));
        final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(imageInfoParsed);
        final ImagePkgMgrDatabase imagePkgMgr = imageInfoDerived.getImageInfoParsed().getPkgMgr();
        imageInfoDerived.setManifestLayerMapping(mapping);
        if (imagePkgMgr != null) {
            imageInfoDerived.setPkgMgrFilePath(determinePkgMgrFilePath(imageInfoDerived.getImageInfoParsed(), imageInfoDerived.getImageInfoParsed().getFileSystemRootDir().getName()));
            imageInfoDerived.setCodeLocationName(Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                    imageInfoDerived.getImageInfoParsed().getPkgMgr().getPackageManager().toString()));
        } else {
            imageInfoDerived.setPkgMgrFilePath(NO_PKG_MGR_FOUND);
            imageInfoDerived.setCodeLocationName(Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                    NO_PKG_MGR_FOUND));
        }
        imageInfoDerived.setFinalProjectName(deriveBlackDuckProject(imageInfoDerived.getManifestLayerMapping().getImageName(), projectName));
        imageInfoDerived.setFinalProjectVersionName(deriveBlackDuckProjectVersion(imageInfoDerived.getManifestLayerMapping(), versionName));
        logger.info(String.format("Black Duck project: %s, version: %s; Code location : %s", imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getCodeLocationName()));
        return imageInfoDerived;
    }

    private String determinePkgMgrFilePath(final ImageInfoParsed imageInfo, final String imageDirectoryName) {
        String pkgMgrFilePath = imageInfo.getPkgMgr().getExtractedPackageManagerDirectory().getAbsolutePath();
        pkgMgrFilePath = pkgMgrFilePath.substring(pkgMgrFilePath.indexOf(imageDirectoryName) + imageDirectoryName.length() + 1);
        return pkgMgrFilePath;
    }

    private void writeBdioToFile(final SimpleBdioDocument bdioDocument, final File bdioOutputFile) throws IOException, FileNotFoundException {
        try (FileOutputStream bdioOutputStream = new FileOutputStream(bdioOutputFile)) {
            try (BdioWriter bdioWriter = new BdioWriter(gson, bdioOutputStream)) {
                BdioGenerator.writeBdio(bdioWriter, bdioDocument);
            }
        }
    }

    private String deriveBlackDuckProject(final String imageName, final String projectName) {
        String blackDuckProjectName;
        if (StringUtils.isBlank(projectName)) {
            blackDuckProjectName = Names.getblackDuckProjectNameFromImageName(imageName);
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
