/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import com.synopsys.integration.exception.IntegrationException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class ImageDirectoryDataExtractorFactoryChooser {

    @NotNull
    public ImageDirectoryDataExtractorFactory choose(List<ImageDirectoryDataExtractorFactory> imageDirectoryDataExtractorFactories, File imageDir) throws IntegrationException {
        ImageDirectoryDataExtractorFactory imageDirectoryDataExtractorFactory = null;
        for (ImageDirectoryDataExtractorFactory candidate : imageDirectoryDataExtractorFactories) {
            if (candidate.applies(imageDir)) {
                imageDirectoryDataExtractorFactory = candidate;
                break;
            }
        }
        if (imageDirectoryDataExtractorFactory == null) {
            throw new IntegrationException("Unrecognized target image format");
        }
        return imageDirectoryDataExtractorFactory;
    }
}
