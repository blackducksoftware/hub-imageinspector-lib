package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;

import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerMetadataExtractor;
import com.synopsys.integration.exception.IntegrationException;

public class OciImageDirectoryDataExtractorFactory implements ImageDirectoryDataExtractorFactory {
    private final OciImageFormatMatchesChecker ociImageFormatMatchesChecker;

    public OciImageDirectoryDataExtractorFactory(final OciImageFormatMatchesChecker ociImageFormatMatchesChecker) {
        this.ociImageFormatMatchesChecker = ociImageFormatMatchesChecker;
    }

    @Override
    public boolean applies(final File imageDir) throws IntegrationException {
        return ociImageFormatMatchesChecker.applies(imageDir);
    }

    @Override
    public ImageDirectoryDataExtractor createImageDirectoryDataExtractor() {
        return null;
    }

    @Override
    public ImageLayerMetadataExtractor createImageLayerMetadataExtractor() {
        return null;
    }
}
