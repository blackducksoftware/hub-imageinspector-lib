/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import java.io.File;

public interface ImageDirectoryDataExtractorFactory {
    boolean applies(File imageDir);
    ImageDirectoryDataExtractor createImageDirectoryDataExtractor();
    ImageLayerMetadataExtractor createImageLayerMetadataExtractor();
}
