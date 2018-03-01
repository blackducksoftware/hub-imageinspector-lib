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
package com.blackducksoftware.integration.hub.imageinspector.imageformat.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.api.PkgMgrDataNotFoundException;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.manifest.Manifest;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.lib.PackageManagerEnum;
import com.blackducksoftware.integration.hub.imageinspector.linux.FileSys;
import com.blackducksoftware.integration.hub.imageinspector.name.Names;

@Component
public class DockerTarParser {
    static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private static final String DOCKER_LAYER_TAR_FILENAME = "layer.tar";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ManifestFactory manifestFactory;

    @Autowired
    public void setManifestFactory(final ManifestFactory manifestFactory) {
        this.manifestFactory = manifestFactory;
    }

    public File extractDockerLayers(final File workingDirectory, final String imageName, final String imageTag, final List<File> layerTars, final List<ManifestLayerMapping> manifestLayerMappings) throws IOException {
        logger.debug(String.format("working dir: %s", workingDirectory));
        final File tarExtractionDirectory = getTarExtractionDirectory(workingDirectory);
        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        File targetImageFileSystemRootDir = null;
        for (final ManifestLayerMapping manifestLayerMapping : manifestLayerMappings) {
            for (final String layer : manifestLayerMapping.getLayers()) {
                logger.trace(String.format("Looking for tar for layer: %s", layer));
                final File layerTar = getLayerTar(layerTars, layer);
                if (layerTar != null) {
                    targetImageFileSystemRootDir = extractLayerTarToDir(imageName, imageTag, targetImageFileSystemParentDir, layerTar, manifestLayerMapping);
                } else {
                    logger.error(String.format("Could not find the tar for layer %s", layer));
                }
            }
        }
        return targetImageFileSystemRootDir;
    }

    public OperatingSystemEnum detectOperatingSystem(final String operatingSystem) {
        OperatingSystemEnum osEnum = null;
        if (StringUtils.isNotBlank(operatingSystem)) {
            osEnum = OperatingSystemEnum.determineOperatingSystem(operatingSystem);
        }
        return osEnum;
    }

    public OperatingSystemEnum detectOperatingSystem(final File targetImageFileSystemRootDir) throws IntegrationException, IOException {
        return deriveOsFromPkgMgr(targetImageFileSystemRootDir);
    }

    public ImageInfoParsed collectPkgMgrInfo(final File targetImageFileSystemRootDir, final OperatingSystemEnum osEnum) throws IntegrationException {
        logger.debug(String.format("Checking image file system at %s for package managers", targetImageFileSystemRootDir.getName()));
        if (osEnum == null) {
            throw new IntegrationException("Operating System value is null");
        }
        for (final PackageManagerEnum packageManagerEnum : PackageManagerEnum.values()) {
            final File packageManagerDirectory = new File(targetImageFileSystemRootDir, packageManagerEnum.getDirectory());
            if (packageManagerDirectory.exists()) {
                logger.info(String.format("Found package Manager Dir: %s", packageManagerDirectory.getAbsolutePath()));
                final ImagePkgMgr targetImagePkgMgr = new ImagePkgMgr(packageManagerDirectory, packageManagerEnum);
                final ImageInfoParsed imagePkgMgrInfo = new ImageInfoParsed(targetImageFileSystemRootDir.getName(), osEnum, targetImagePkgMgr);
                return imagePkgMgrInfo;
            } else {
                logger.debug(String.format("Package manager dir %s does not exist", packageManagerDirectory.getAbsolutePath()));
            }
        }
        throw new IntegrationException("No package manager files found in this Docker image.");

    }

    public List<File> extractLayerTars(final File workingDirectory, final File dockerTar) throws IOException {
        logger.debug(String.format("working dir: %s", workingDirectory));
        final File tarExtractionDirectory = getTarExtractionDirectory(workingDirectory);
        final List<File> untaredFiles = new ArrayList<>();
        final File outputDir = new File(tarExtractionDirectory, dockerTar.getName());
        final TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(dockerTar));
        try {
            TarArchiveEntry tarArchiveEntry = null;
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                final File outputFile = new File(outputDir, tarArchiveEntry.getName());
                if (tarArchiveEntry.isFile()) {
                    if (!outputFile.getParentFile().exists()) {
                        outputFile.getParentFile().mkdirs();
                    }
                    final OutputStream outputFileStream = new FileOutputStream(outputFile);
                    try {
                        IOUtils.copy(tarArchiveInputStream, outputFileStream);
                        if (tarArchiveEntry.getName().contains(DOCKER_LAYER_TAR_FILENAME)) {
                            untaredFiles.add(outputFile);
                        }
                    } finally {
                        outputFileStream.close();
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream);
        }
        return untaredFiles;
    }

    public List<ManifestLayerMapping> getLayerMappings(final File workingDirectory, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        logger.debug(String.format("getLayerMappings(): dockerImageName: %s; dockerTagName: %s", dockerImageName, dockerTagName));
        logger.debug(String.format("working dir: %s", workingDirectory));
        final Manifest manifest = manifestFactory.createManifest(getTarExtractionDirectory(workingDirectory), tarFileName);
        List<ManifestLayerMapping> mappings;
        try {
            mappings = manifest.getLayerMappings(dockerImageName, dockerTagName);
        } catch (final Exception e) {
            final String msg = String.format("Could not parse the image manifest file : %s", e.getMessage());
            logger.error(msg);
            throw new IntegrationException(msg, e);
        }
        if (mappings.size() == 0) {
            final String msg = String.format("Could not find image %s:%s in tar file %s", dockerImageName, dockerTagName, tarFileName);
            throw new IntegrationException(msg);
        }
        return mappings;
    }

    private File getTarExtractionDirectory(final File workingDirectory) {
        return new File(workingDirectory, TAR_EXTRACTION_DIRECTORY);
    }

    private File extractLayerTarToDir(final String imageName, final String imageTag, final File imageFilesDir, final File layerTar, final ManifestLayerMapping mapping) throws IOException {
        logger.trace(String.format("Extracting layer: %s into %s", layerTar.getAbsolutePath(), Names.getTargetImageFileSystemRootDirName(imageName, imageTag)));
        final File targetImageFileSystemRoot = new File(imageFilesDir, Names.getTargetImageFileSystemRootDirName(imageName, imageTag));
        final DockerLayerTar dockerLayerTar = new DockerLayerTar(layerTar);
        dockerLayerTar.extractToDir(targetImageFileSystemRoot);
        return targetImageFileSystemRoot;
    }

    private File getLayerTar(final List<File> layerTars, final String layer) {
        File layerTar = null;
        for (final File candidateLayerTar : layerTars) {
            if (layer.equals(candidateLayerTar.getParentFile().getName())) {
                logger.debug(String.format("Found layer tar for layer %s", layer));
                layerTar = candidateLayerTar;
                break;
            }
        }
        return layerTar;
    }

    private OperatingSystemEnum deriveOsFromPkgMgr(final File targetImageFileSystemRootDir) throws PkgMgrDataNotFoundException {
        OperatingSystemEnum osEnum = null;

        final FileSys extractedFileSys = new FileSys(targetImageFileSystemRootDir);
        final Set<PackageManagerEnum> packageManagers = extractedFileSys.getPackageManagers();
        if (packageManagers.size() == 1) {
            final PackageManagerEnum packageManager = packageManagers.iterator().next();
            osEnum = packageManager.getOperatingSystem();
            logger.debug(String.format("Package manager %s returns Operating System %s", packageManager.name(), osEnum.name()));
            return osEnum;
        } else if (packageManagers.size() == 0) {
            throw new PkgMgrDataNotFoundException("No package manager data found");
        } else {
            throw new PkgMgrDataNotFoundException("Data found for more than one package manager");
        }
    }
}
