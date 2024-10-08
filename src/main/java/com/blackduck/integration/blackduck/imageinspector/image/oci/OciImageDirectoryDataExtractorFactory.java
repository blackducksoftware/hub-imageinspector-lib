/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci;

import java.io.File;

import com.blackduck.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import com.blackduck.integration.blackduck.imageinspector.image.common.*;
import com.google.gson.Gson;
import com.blackduck.integration.blackduck.imageinspector.image.common.*;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.exception.IntegrationException;

public class OciImageDirectoryDataExtractorFactory implements ImageDirectoryDataExtractorFactory {
    private final OciImageFormatMatchesChecker ociImageFormatMatchesChecker;
    private final CommonImageConfigParser commonImageConfigParser;
    private final Gson gson;

    public OciImageDirectoryDataExtractorFactory(final OciImageFormatMatchesChecker ociImageFormatMatchesChecker, final CommonImageConfigParser commonImageConfigParser, final Gson gson) {
        this.ociImageFormatMatchesChecker = ociImageFormatMatchesChecker;
        this.commonImageConfigParser = commonImageConfigParser;
        this.gson = gson;
    }

    @Override
    public boolean applies(final File imageDir) throws IntegrationException {
        return ociImageFormatMatchesChecker.applies(imageDir);
    }

    @Override
    public ImageDirectoryDataExtractor createImageDirectoryDataExtractor() {
        FileOperations fileOperations = new FileOperations();
        OciImageIndexFileParser ociImageIndexFileParser = new OciImageIndexFileParser(gson, fileOperations);
        OciManifestDescriptorParser ociManifestDescriptorParser = new OciManifestDescriptorParser(new ManifestRepoTagMatcher());
        ImageNameResolver imageNameResolver = new ImageNameResolver();
        OciImageDirectoryExtractor ociImageDirectoryExtractor = new OciImageDirectoryExtractor(gson, fileOperations, imageNameResolver, commonImageConfigParser, ociImageIndexFileParser, ociManifestDescriptorParser);
        ImageLayerSorter imageOrderedLayerExtractor = new OciImageLayerSorter();
        LayerDataExtractor layerDataExtractor = new LayerDataExtractor(imageOrderedLayerExtractor);
        return new ImageDirectoryDataExtractor(ociImageDirectoryExtractor, layerDataExtractor);
    }

    @Override
    public ImageLayerMetadataExtractor createImageLayerMetadataExtractor() {
        OciImageConfigCommandParser configCommandParser = new OciImageConfigCommandParser(commonImageConfigParser);
        return new OciImageLayerMetadataExtractor(configCommandParser);
    }
}
