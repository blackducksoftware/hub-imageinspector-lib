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
    private final GsonBuilder gsonBuilder;

    public DockerImageDirectoryDataExtractorFactory(final DockerImageFormatMatchesChecker dockerImageFormatMatchesChecker, final CommonImageConfigParser commonImageConfigParser, final GsonBuilder gsonBuilder) {
        this.dockerImageFormatMatchesChecker = dockerImageFormatMatchesChecker;
        this.commonImageConfigParser = commonImageConfigParser;
        this.gsonBuilder = gsonBuilder;
    }

    @Override
    public boolean applies(File imageDir) {
        return dockerImageFormatMatchesChecker.applies(imageDir);
    }

    @Override
    public ImageDirectoryDataExtractor createImageDirectoryDataExtractor() {
        FileOperations fileOperations = new FileOperations();
        DockerManifestFactory dockerManifestFactory = new DockerManifestFactory();
        ImageDirectoryExtractor imageDirectoryExtractor = new DockerImageDirectoryExtractor(gsonBuilder, fileOperations, commonImageConfigParser, dockerManifestFactory);
        ImageOrderedLayerExtractor imageOrderedLayerExtractor = new ImageOrderedLayerExtractor();
        return new ImageDirectoryDataExtractor(imageDirectoryExtractor, imageOrderedLayerExtractor);
    }

    @Override
    public ImageLayerMetadataExtractor createImageLayerMetadataExtractor() {
        DockerImageLayerConfigParser dockerImageLayerConfigParser = new DockerImageLayerConfigParser(commonImageConfigParser);
        return new DockerImageLayerMetadataExtractor(dockerImageLayerConfigParser);
    }
}