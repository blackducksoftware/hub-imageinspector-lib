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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGeneratorFactory;
import com.synopsys.integration.blackduck.imageinspector.name.Names;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioWriter;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

@Component
public class ImageInspector {
    private static final String NO_PKG_MGR_FOUND = "noPkgMgr";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DockerTarParser tarParser;

    @Autowired
    private BdioGeneratorFactory bdioGeneratorFactory;

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

    public ImageInfoParsed detectInspectorOperatingSystem(final File targetImageFileSystemRootDir) throws IntegrationException, IOException {
        return tarParser.collectPkgMgrInfo(targetImageFileSystemRootDir);
    }

    public List<ManifestLayerMapping> getLayerMappings(final File workingDir, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        return tarParser.getLayerMappings(workingDir, tarFileName, dockerImageName, dockerTagName);
    }

    public ImageInfoDerived generateBdioFromImageFilesDir(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> mappings, final String projectName, final String versionName, final File dockerTar,
            final File targetImageFileSystemRootDir, final String codeLocationPrefix, final boolean usePreferredAliasNamespaceForge) throws IOException, IntegrationException, InterruptedException {
        final ImageInfoParsed imageInfoParsed = tarParser.collectPkgMgrInfo(targetImageFileSystemRootDir);
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(dockerImageRepo, dockerImageTag, mappings, projectName, versionName, targetImageFileSystemRootDir, codeLocationPrefix, imageInfoParsed);
        final BdioGenerator bdioGenerator = bdioGeneratorFactory.createExtractor(targetImageFileSystemRootDir, imageInfoDerived.getImageInfoParsed().getPkgMgr().getPackageManager());

        // TODO: To commit to usePreferredAliasNamespaceForge, remove this if
        String extractedLinuxDistroName = null;
        if (usePreferredAliasNamespaceForge) {
            extractedLinuxDistroName = imageInfoDerived.getImageInfoParsed().getLinuxDistroName();
        }
        final SimpleBdioDocument bdioDocument = bdioGenerator.extract(dockerImageRepo, dockerImageTag, imageInfoDerived.getCodeLocationName(),
                imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), extractedLinuxDistroName);
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    public ImageInfoDerived generateEmptyBdio(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> mappings, final String projectName, final String versionName, final File dockerTar,
            final File targetImageFileSystemRootDir, final String codeLocationPrefix) throws IOException, IntegrationException, InterruptedException {
        final ImageInfoParsed imageInfoParsed = new ImageInfoParsed(targetImageFileSystemRootDir.getName(), null, null);
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(dockerImageRepo, dockerImageTag, mappings, projectName, versionName, targetImageFileSystemRootDir, codeLocationPrefix, imageInfoParsed);
        final BdioGenerator bdioGenerator = bdioGeneratorFactory.createExtractor(null, PackageManagerEnum.NULL);
        final SimpleBdioDocument bdioDocument = bdioGenerator.extract(dockerImageRepo, dockerImageTag, imageInfoDerived.getCodeLocationName(), imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), null);
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
            final String codeLocationPrefix, final ImageInfoParsed imageInfoParsed) throws IntegrationException, IOException {
        logger.debug(String.format("generateBdioFromImageFilesDir(): projectName: %s, versionName: %s", projectName, versionName));
        final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(imageInfoParsed);
        final ImagePkgMgrDatabase imagePkgMgr = imageInfoDerived.getImageInfoParsed().getPkgMgr();
        imageInfoDerived.setImageDirName(Names.getTargetImageFileSystemRootDirName(dockerImageRepo, dockerImageTag));
        imageInfoDerived.setManifestLayerMapping(findManifestLayerMapping(mappings, imageInfoDerived.getImageInfoParsed(), imageInfoDerived.getImageDirName()));
        if (imagePkgMgr != null) {
            imageInfoDerived.setPkgMgrFilePath(determinePkgMgrFilePath(imageInfoDerived.getImageInfoParsed(), imageInfoDerived.getImageDirName()));
            imageInfoDerived.setCodeLocationName(Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                    imageInfoDerived.getPkgMgrFilePath(), imageInfoDerived.getImageInfoParsed().getPkgMgr().getPackageManager().toString()));
        } else {
            imageInfoDerived.setPkgMgrFilePath(NO_PKG_MGR_FOUND);
            imageInfoDerived.setCodeLocationName(Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                    imageInfoDerived.getPkgMgrFilePath(), NO_PKG_MGR_FOUND));
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
