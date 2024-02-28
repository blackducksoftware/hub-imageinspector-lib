/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.docker;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.image.docker.manifest.DockerManifest;
import com.synopsys.integration.blackduck.imageinspector.image.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerImageDirectoryExtractor implements ImageDirectoryExtractor {
    private static final String DOCKER_LAYER_TAR_FILENAME = "layer.tar";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Gson gson;
    private final FileOperations fileOperations;
    private final CommonImageConfigParser commonImageConfigParser;
    private final DockerManifestFactory dockerManifestFactory;

    public DockerImageDirectoryExtractor(Gson gson, FileOperations fileOperations, CommonImageConfigParser commonImageConfigParser, DockerManifestFactory dockerManifestFactory) {
        this.gson = gson;
        this.fileOperations = fileOperations;
        this.commonImageConfigParser = commonImageConfigParser;
        this.dockerManifestFactory = dockerManifestFactory;
    }

    @Override
    public List<TypedArchiveFile> getLayerArchives(File imageDir, @Nullable String givenRepo, @Nullable String givenTag) throws IntegrationException {
        logger.debug(String.format("Searching for layer archive files in unpackedImageDir: %s", imageDir.getAbsolutePath()));
        final List<TypedArchiveFile> untaredLayerFiles = new ArrayList<>();
        List<File> unpackedImageTopLevelFiles = Arrays.asList(imageDir.listFiles());
        for (File unpackedImageTopLevelFile : unpackedImageTopLevelFiles) {
            if (unpackedImageTopLevelFile.isDirectory()) {
                List<File> unpackedImageSecondLevelFiles = Arrays.asList(unpackedImageTopLevelFile.listFiles());
                for (File unpackedImageSecondLevelFile : unpackedImageSecondLevelFiles) {
                    if (unpackedImageSecondLevelFile.isFile() && unpackedImageSecondLevelFile.getName().equals(DOCKER_LAYER_TAR_FILENAME)) {
                        TypedArchiveFile typedArchiveFile = new TypedArchiveFile(ArchiveFileType.TAR, unpackedImageSecondLevelFile);
                        untaredLayerFiles.add(typedArchiveFile);
                    }
                }
            }
        }
        return untaredLayerFiles;
    }

    @Override
    public FullLayerMapping getLayerMapping(File imageDir, final String repo, final String tag) throws IntegrationException {
        final DockerManifest manifest = dockerManifestFactory.createManifest(imageDir);
        ManifestLayerMapping manifestLayerMapping;
        try {
            manifestLayerMapping = manifest.getLayerMapping(repo, tag);
        } catch (final Exception e) {
            final String msg = String.format("Could not parse the image manifest file : %s", e.getMessage());
            logger.error(msg);
            throw new IntegrationException(msg, e);
        }
        final List<String> externalLayerIds = commonImageConfigParser.getExternalLayerIdsFromImageConfigFile(imageDir, manifestLayerMapping.getPathToImageConfigFileFromRoot());
        return new FullLayerMapping(manifestLayerMapping, externalLayerIds);
    }

    private List<String> getExternalLayerIdsFromImageConfigFile(File imageDir, String imageConfigFileName) {
        try {
            final File imageConfigFile = new File(imageDir, imageConfigFileName);
            final String imageConfigFileContents = fileOperations
                    .readFileToString(imageConfigFile);
            logger.trace(String.format("imageConfigFileContents (%s): %s", imageConfigFile.getName(), imageConfigFileContents));
            return commonImageConfigParser.parseExternalLayerIds(imageConfigFileContents);
        } catch (Exception e) {
            logger.warn(String.format("Error logging image config file contents: %s", e.getMessage()));
        }
        return new ArrayList<>(0);
    }

}
