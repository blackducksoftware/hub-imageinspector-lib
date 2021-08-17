/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.docker;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.*;
import com.synopsys.integration.blackduck.imageinspector.image.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

import java.io.File;

public class DockerImageDirectoryDataExtractorFactory implements ImageDirectoryDataExtractorFactory {
    private final DockerImageFormatMatchesChecker dockerImageFormatMatchesChecker;
    private final CommonImageConfigParser commonImageConfigParser;

    public DockerImageDirectoryDataExtractorFactory(final DockerImageFormatMatchesChecker dockerImageFormatMatchesChecker, final CommonImageConfigParser commonImageConfigParser) {
        this.dockerImageFormatMatchesChecker = dockerImageFormatMatchesChecker;
        this.commonImageConfigParser = commonImageConfigParser;
    }

    @Override
    public boolean applies(File imageDir) {
        return dockerImageFormatMatchesChecker.applies(imageDir);
    }

    @Override
    public ImageDirectoryDataExtractor createImageDirectoryDataExtractor() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        FileOperations fileOperations = new FileOperations();
        DockerImageConfigParser dockerImageConfigParser = new DockerImageConfigParser(commonImageConfigParser);
        DockerManifestFactory dockerManifestFactory = new DockerManifestFactory();
        ImageDirectoryExtractor imageDirectoryExtractor = new DockerImageDirectoryExtractor(gsonBuilder, fileOperations, dockerImageConfigParser, dockerManifestFactory);
        ImageOrderedLayerExtractor imageOrderedLayerExtractor = new ImageOrderedLayerExtractor();
        return new ImageDirectoryDataExtractor(imageDirectoryExtractor, imageOrderedLayerExtractor);
    }

    @Override
    public ImageLayerMetadataExtractor createImageLayerMetadataExtractor() {
        DockerImageLayerConfigParser dockerImageLayerConfigParser = new DockerImageLayerConfigParser(commonImageConfigParser);
        return new DockerImageLayerMetadataExtractor(dockerImageLayerConfigParser);
    }
}
