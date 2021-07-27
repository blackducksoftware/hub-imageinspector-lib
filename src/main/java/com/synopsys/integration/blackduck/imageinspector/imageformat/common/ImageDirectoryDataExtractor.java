/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.lib.FullLayerMapping;
import com.synopsys.integration.exception.IntegrationException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageDirectoryDataExtractor {
    private final ImageFormatMatchesChecker imageFormatMatchesChecker;
    private final ImageDirectoryExtractor imageDirectoryExtractor;
    private final ImageOrderedLayerExtractor imageOrderedLayerExtractor;

    public ImageDirectoryDataExtractor(ImageFormatMatchesChecker imageFormatMatchesChecker, ImageDirectoryExtractor imageDirectoryExtractor, ImageOrderedLayerExtractor imageOrderedLayerExtractor) {
        this.imageFormatMatchesChecker = imageFormatMatchesChecker;
        this.imageDirectoryExtractor = imageDirectoryExtractor;
        this.imageOrderedLayerExtractor = imageOrderedLayerExtractor;
    }

    public boolean applies(File imageDir) throws IntegrationException {
        return imageFormatMatchesChecker.applies(imageDir);
    }

    public ImageDirectoryData extract(File imageDir, String givenRepo, String givenTag) throws IOException, IntegrationException {
        List<TypedArchiveFile> unOrderedLayerArchives = imageDirectoryExtractor.getLayerArchives(imageDir);
        FullLayerMapping fullLayerMapping = imageDirectoryExtractor.getLayerMapping(imageDir, givenRepo, givenTag);
        List<TypedArchiveFile> orderedLayerArchives = imageOrderedLayerExtractor.getOrderedLayerArchives(unOrderedLayerArchives, fullLayerMapping.getManifestLayerMapping());
        return new ImageDirectoryData(fullLayerMapping.getManifestLayerMapping().getImageName(), fullLayerMapping.getManifestLayerMapping().getTagName(),
                fullLayerMapping, orderedLayerArchives);
    }
}
