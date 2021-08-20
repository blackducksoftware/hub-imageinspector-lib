/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.exception.IntegrationException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ImageDirectoryDataExtractor {
    private final ImageDirectoryExtractor imageDirectoryExtractor;
    private final ImageOrderedLayerExtractor imageOrderedLayerExtractor;

    public ImageDirectoryDataExtractor(ImageDirectoryExtractor imageDirectoryExtractor, ImageOrderedLayerExtractor imageOrderedLayerExtractor) {
        this.imageDirectoryExtractor = imageDirectoryExtractor;
        this.imageOrderedLayerExtractor = imageOrderedLayerExtractor;
    }

    public ImageDirectoryData extract(File imageDir, String givenRepo, String givenTag) throws IOException, IntegrationException {
        List<TypedArchiveFile> unOrderedLayerArchives = imageDirectoryExtractor.getLayerArchives(imageDir);
        FullLayerMapping fullLayerMapping = imageDirectoryExtractor.getLayerMapping(imageDir, givenRepo, givenTag);
        List<TypedArchiveFile> orderedLayerArchives = imageOrderedLayerExtractor.getOrderedLayerArchives(unOrderedLayerArchives, fullLayerMapping.getManifestLayerMapping());
        List<LayerDetailsBuilder> layerData = collectLayerData(fullLayerMapping, orderedLayerArchives);
        return new ImageDirectoryData(fullLayerMapping.getManifestLayerMapping().getImageName(), fullLayerMapping.getManifestLayerMapping().getTagName(), fullLayerMapping, layerData);
    }

    private List<LayerDetailsBuilder> collectLayerData(FullLayerMapping layerMapping, List<TypedArchiveFile> archives) {
        List<LayerDetailsBuilder> layerData = new LinkedList<>();
        for (int layerIndex = 0; layerIndex < archives.size(); layerIndex++) {
            layerData.add(new LayerDetailsBuilder(layerIndex, archives.get(layerIndex), layerMapping.getLayerExternalId(layerIndex)));
        }
        return layerData;
    }
}
