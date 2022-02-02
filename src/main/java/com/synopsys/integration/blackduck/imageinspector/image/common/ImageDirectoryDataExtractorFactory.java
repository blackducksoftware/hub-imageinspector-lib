/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import java.io.File;

import com.synopsys.integration.exception.IntegrationException;

public interface ImageDirectoryDataExtractorFactory {
    boolean applies(File imageDir) throws IntegrationException;
    ImageDirectoryDataExtractor createImageDirectoryDataExtractor();
    ImageLayerMetadataExtractor createImageLayerMetadataExtractor();
}
