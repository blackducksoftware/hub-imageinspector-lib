/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.*;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageDirectoryDataExtractorFactory {
    private final List<ImageDirectoryDataExtractor> extractors = new ArrayList<>();

//    public ImageDirectoryDataExtractorFactory(List<ImageDirectoryDataExtractor> extractors) {
//        this.extractors = extractors;
//    }

    // TODO someone needs to create the list; someone needs to run each applies() method
//    public ImageDirectoryDataExtractor create(File imageDir) throws IntegrationException {
//        for (ImageDirectoryDataExtractor candidateExtractor : extractors) {
//            if (candidateExtractor.applies(imageDir)) {
//                return candidateExtractor;
//            }
//        }
//        throw new IntegrationException("No ImageDirectoryDataExtractor applied; unrecognized image format");
//    }

    // TODO these two methods don't belong together
    // TODO this is temp building only Docker stuff
    public ImageDirectoryDataExtractor createImageDirectoryDataExtractor() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        FileOperations fileOperations = new FileOperations();
        DockerImageConfigParser dockerImageConfigParser = new DockerImageConfigParser();
        DockerManifestFactory dockerManifestFactory = new DockerManifestFactory();
        ImageDirectoryExtractor imageDirectoryExtractor = new DockerImageDirectoryExtractor(gsonBuilder, fileOperations, dockerImageConfigParser, dockerManifestFactory);
        ImageOrderedLayerExtractor imageOrderedLayerExtractor = new ImageOrderedLayerExtractor();
        ImageFormatMatchesChecker imageFormatMatchesChecker = new DockerImageFormatMatchesChecker();
        return new ImageDirectoryDataExtractor(imageFormatMatchesChecker, imageDirectoryExtractor, imageOrderedLayerExtractor);
    }

    public DockerImageLayerMetadataExtractor createDockerImageLayerMetadataExtractor() {
        DockerImageLayerConfigParser dockerImageLayerConfigParser = new DockerImageLayerConfigParser(new GsonBuilder());
        return new DockerImageLayerMetadataExtractor(dockerImageLayerConfigParser);
    }
}
