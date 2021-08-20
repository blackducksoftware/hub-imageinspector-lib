/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerMetadataExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerSorter;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;

public class OciImageDirectoryDataExtractorFactory implements ImageDirectoryDataExtractorFactory {
    private final OciImageFormatMatchesChecker ociImageFormatMatchesChecker;
    private final CommonImageConfigParser commonImageConfigParser;
    private final GsonBuilder gsonBuilder;

    public OciImageDirectoryDataExtractorFactory(final OciImageFormatMatchesChecker ociImageFormatMatchesChecker, final CommonImageConfigParser commonImageConfigParser, final GsonBuilder gsonBuilder) {
        this.ociImageFormatMatchesChecker = ociImageFormatMatchesChecker;
        this.commonImageConfigParser = commonImageConfigParser;
        this.gsonBuilder = gsonBuilder;
    }

    @Override
    public boolean applies(final File imageDir) throws IntegrationException {
        return ociImageFormatMatchesChecker.applies(imageDir);
    }

    @Override
    public ImageDirectoryDataExtractor createImageDirectoryDataExtractor() {
        FileOperations fileOperations = new FileOperations();
        OciImageDirectoryExtractor ociImageDirectoryExtractor = new OciImageDirectoryExtractor(gsonBuilder, fileOperations, commonImageConfigParser);
        ImageLayerSorter imageOrderedLayerExtractor = new OciImageLayerSorter();
        return new ImageDirectoryDataExtractor(ociImageDirectoryExtractor, imageOrderedLayerExtractor);
    }

    @Override
    public ImageLayerMetadataExtractor createImageLayerMetadataExtractor() {
        OciImageConfigCommandParser configCommandParser = new OciImageConfigCommandParser(commonImageConfigParser);
        return new OciImageLayerMetadataExtractor(configCommandParser);
    }
}
