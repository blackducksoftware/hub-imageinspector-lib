/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import java.io.File;

// TODO these factories should create ALL image-specific classes; better than Spring dependency injection
public interface ImageDirectoryDataExtractorFactory {
    boolean applies(File imageDir);
    ImageDirectoryDataExtractor createImageDirectoryDataExtractor();
    ImageLayerMetadataExtractor createImageLayerMetadataExtractor();
}
