/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import com.blackduck.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.blackduck.integration.exception.IntegrationException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageDirectoryDataExtractor {
    private final ImageDirectoryExtractor imageDirectoryExtractor;
    private final LayerDataExtractor layerDataExtractor;

    public ImageDirectoryDataExtractor(final ImageDirectoryExtractor imageDirectoryExtractor, final LayerDataExtractor layerDataExtractor) {
        this.imageDirectoryExtractor = imageDirectoryExtractor;
        this.layerDataExtractor = layerDataExtractor;
    }

    public ImageDirectoryData extract(File imageDir, @Nullable String givenRepo, @Nullable String givenTag) throws IOException, IntegrationException {
        List<TypedArchiveFile> unOrderedLayerArchives = imageDirectoryExtractor.getLayerArchives(imageDir, givenRepo, givenTag);
        FullLayerMapping fullLayerMapping = imageDirectoryExtractor.getLayerMapping(imageDir, givenRepo, givenTag);
        List<LayerDetailsBuilder> layerData = layerDataExtractor.getLayerData(unOrderedLayerArchives, fullLayerMapping);
        return new ImageDirectoryData(fullLayerMapping.getManifestLayerMapping().getImageName().orElse(null), fullLayerMapping.getManifestLayerMapping().getTagName().orElse(null), fullLayerMapping, layerData);
    }
}
