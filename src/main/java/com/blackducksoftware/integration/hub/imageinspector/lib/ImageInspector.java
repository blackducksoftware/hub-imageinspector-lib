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
package com.blackducksoftware.integration.hub.imageinspector.lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.bdio.BdioWriter;
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.DockerTarParser;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.ImageInfoParsed;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.ImagePkgMgr;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.imageinspector.linux.FileOperations;
import com.blackducksoftware.integration.hub.imageinspector.linux.extractor.Extractor;
import com.blackducksoftware.integration.hub.imageinspector.linux.extractor.ExtractorManager;
import com.blackducksoftware.integration.hub.imageinspector.linux.extractor.NullExtractor;
import com.blackducksoftware.integration.hub.imageinspector.name.Names;
import com.google.gson.Gson;

@Component
public class ImageInspector {
    private static final String NO_PKG_MGR_FOUND = "noPkgMgr";
    private static final String UNKNOWN_ARCH = "unknown";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ExtractorManager extractorManager;
    private DockerTarParser tarParser;

    @Autowired
    private NullExtractor nullExtractor;

    @Autowired
    public void setExtractorManager(final ExtractorManager extractorManager) {
        this.extractorManager = extractorManager;
    }

    @Autowired
    public void setTarParser(final DockerTarParser tarParser) {
        this.tarParser = tarParser;
    }

    public List<File> extractLayerTars(final File workingDir, final File dockerTar) throws IOException {
        return tarParser.extractLayerTars(workingDir, dockerTar);
    }

    public File extractDockerLayers(final File workingDir, final String imageName, final String imageTag, final List<File> layerTars, final List<ManifestLayerMapping> layerMappings) throws IOException {
        return tarParser.extractDockerLayers(workingDir, imageName, imageTag, layerTars, layerMappings);
    }

    public OperatingSystemEnum detectOperatingSystem(final String operatingSystem) {
        return tarParser.detectOperatingSystem(operatingSystem);
    }

    public OperatingSystemEnum detectInspectorOperatingSystem(final File targetImageFileSystemRootDir) throws IntegrationException, IOException {
        return tarParser.detectInspectorOperatingSystem(targetImageFileSystemRootDir);
    }

    public List<ManifestLayerMapping> getLayerMappings(final File workingDir, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        return tarParser.getLayerMappings(workingDir, tarFileName, dockerImageName, dockerTagName);
    }

    public ImageInfoDerived generateBdioFromImageFilesDir(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> mappings, final String projectName, final String versionName, final File dockerTar,
            final File targetImageFileSystemRootDir, final OperatingSystemEnum osEnum, final String codeLocationPrefix) throws IOException, IntegrationException, InterruptedException {
        final ImageInfoParsed imageInfoParsed = tarParser.collectPkgMgrInfo(targetImageFileSystemRootDir, osEnum);
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(dockerImageRepo, dockerImageTag, mappings, projectName, versionName, targetImageFileSystemRootDir, osEnum, codeLocationPrefix, imageInfoParsed);
        final Extractor extractor = getExtractorByPackageManager(imageInfoDerived.getImageInfoParsed().getPkgMgr().getPackageManager());
        final SimpleBdioDocument bdioDocument = extractor.extract(dockerImageRepo, dockerImageTag, imageInfoDerived.getImageInfoParsed().getPkgMgr(), imageInfoDerived.getArchitecture(), imageInfoDerived.getCodeLocationName(),
                imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName());
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    public ImageInfoDerived generateEmptyBdio(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> mappings, final String projectName, final String versionName, final File dockerTar,
            final File targetImageFileSystemRootDir, final OperatingSystemEnum osEnum, final String codeLocationPrefix) throws IOException, IntegrationException, InterruptedException {
        final ImageInfoParsed imageInfoParsed = new ImageInfoParsed(targetImageFileSystemRootDir.getName(), null, null);
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(dockerImageRepo, dockerImageTag, mappings, projectName, versionName, targetImageFileSystemRootDir, osEnum, codeLocationPrefix, imageInfoParsed);
        final Extractor extractor = nullExtractor;
        final SimpleBdioDocument bdioDocument = extractor.createEmptyBdio(imageInfoDerived.getCodeLocationName(),
                imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName());
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    public File writeBdioFile(final File outputDirectory, final ImageInfoDerived imageInfoDerived) throws FileNotFoundException, IOException {
        final String bdioFilename = Names.getBdioFilename(imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getPkgMgrFilePath(), imageInfoDerived.getFinalProjectName(),
                imageInfoDerived.getFinalProjectVersionName());
        FileOperations.ensureDirExists(outputDirectory);
        final File bdioOutputFile = new File(outputDirectory, bdioFilename);
        writeBdioToFile(imageInfoDerived.getBdioDocument(), bdioOutputFile);
        return bdioOutputFile;
    }

    private ImageInfoDerived deriveImageInfo(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> mappings, final String projectName, final String versionName, final File targetImageFileSystemRootDir,
            final OperatingSystemEnum osEnum, final String codeLocationPrefix, final ImageInfoParsed imageInfoParsed) throws IntegrationException, IOException {
        logger.debug(String.format("generateBdioFromImageFilesDir(): projectName: %s, versionName: %s", projectName, versionName));
        final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(imageInfoParsed);
        final ImagePkgMgr imagePkgMgr = imageInfoDerived.getImageInfoParsed().getPkgMgr();
        imageInfoDerived.setImageDirName(Names.getTargetImageFileSystemRootDirName(dockerImageRepo, dockerImageTag));
        imageInfoDerived.setManifestLayerMapping(findManifestLayerMapping(mappings, imageInfoDerived.getImageInfoParsed(), imageInfoDerived.getImageDirName()));
        if (imagePkgMgr != null) {
            imageInfoDerived.setArchitecture(getExtractorByPackageManager(imagePkgMgr.getPackageManager()).deriveArchitecture(targetImageFileSystemRootDir));
            imageInfoDerived.setPkgMgrFilePath(determinePkgMgrFilePath(imageInfoDerived.getImageInfoParsed(), imageInfoDerived.getImageDirName()));
            imageInfoDerived.setCodeLocationName(Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                    imageInfoDerived.getPkgMgrFilePath(), imageInfoDerived.getImageInfoParsed().getPkgMgr().getPackageManager().toString()));
        } else {
            imageInfoDerived.setPkgMgrFilePath(NO_PKG_MGR_FOUND);
            imageInfoDerived.setCodeLocationName(Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                    imageInfoDerived.getPkgMgrFilePath(), NO_PKG_MGR_FOUND));
            imageInfoDerived.setArchitecture(UNKNOWN_ARCH);
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

    private ManifestLayerMapping findManifestLayerMapping(final List<ManifestLayerMapping> layerMappings, final ImageInfoParsed imageInfo, final String imageDirectoryName) throws IntegrationException {
        ManifestLayerMapping manifestMapping = null;
        for (final ManifestLayerMapping mapping : layerMappings) {
            if (StringUtils.compare(imageDirectoryName, imageInfo.getFileSystemRootDirName()) == 0) {
                manifestMapping = mapping;
            }
        }
        if (manifestMapping == null) {
            throw new IntegrationException(String.format("Mapping for %s not found in target image manifest file", imageInfo.getFileSystemRootDirName()));
        }
        return manifestMapping;
    }

    private void writeBdioToFile(final SimpleBdioDocument bdioDocument, final File bdioOutputFile) throws IOException, FileNotFoundException {
        try (FileOutputStream bdioOutputStream = new FileOutputStream(bdioOutputFile)) {
            try (BdioWriter bdioWriter = new BdioWriter(new Gson(), bdioOutputStream)) {
                Extractor.writeBdio(bdioWriter, bdioDocument);
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

    private Extractor getExtractorByPackageManager(final PackageManagerEnum packageManagerEnum) throws IntegrationException {
        for (final Extractor currentExtractor : extractorManager.getExtractors()) {
            if (currentExtractor.getPackageManagerEnum() == packageManagerEnum) {
                return currentExtractor;
            }
        }
        throw new IntegrationException(String.format("Extractor not found for packageManager %s", packageManagerEnum.toString()));
    }
}
