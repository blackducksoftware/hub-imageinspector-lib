/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import com.synopsys.integration.exception.IntegrationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
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
