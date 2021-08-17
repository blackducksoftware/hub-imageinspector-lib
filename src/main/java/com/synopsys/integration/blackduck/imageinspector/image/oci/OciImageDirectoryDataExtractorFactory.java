package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerMetadataExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageOrderedLayerExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;

public class OciImageDirectoryDataExtractorFactory implements ImageDirectoryDataExtractorFactory {
    private final OciImageFormatMatchesChecker ociImageFormatMatchesChecker;
    private final CommonImageConfigParser commonImageConfigParser;

    public OciImageDirectoryDataExtractorFactory(final OciImageFormatMatchesChecker ociImageFormatMatchesChecker, final CommonImageConfigParser commonImageConfigParser) {
        this.ociImageFormatMatchesChecker = ociImageFormatMatchesChecker;
        this.commonImageConfigParser = commonImageConfigParser;
    }

    @Override
    public boolean applies(final File imageDir) throws IntegrationException {
        return ociImageFormatMatchesChecker.applies(imageDir);
    }

    @Override
    public ImageDirectoryDataExtractor createImageDirectoryDataExtractor() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        FileOperations fileOperations = new FileOperations();
        OciImageConfigParser ociImageConfigParser = new OciImageConfigParser(commonImageConfigParser);
        OciImageDirectoryExtractor ociImageDirectoryExtractor = new OciImageDirectoryExtractor(gsonBuilder, fileOperations, ociImageConfigParser);
        ImageOrderedLayerExtractor imageOrderedLayerExtractor = new ImageOrderedLayerExtractor(); // TODO- Is this cool?
        return new ImageDirectoryDataExtractor(ociImageDirectoryExtractor, imageOrderedLayerExtractor);
    }

    @Override
    public ImageLayerMetadataExtractor createImageLayerMetadataExtractor() {
        OciImageConfigCommandParser configCommandParser = new OciImageConfigCommandParser(commonImageConfigParser);
        return new OciImageLayerMetadataExtractor(configCommandParser);
    }
}
