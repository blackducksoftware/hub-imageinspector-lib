/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import com.synopsys.integration.blackduck.imageinspector.image.common.ImageFormatMatchesChecker;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class OciImageFormatMatchesChecker implements ImageFormatMatchesChecker {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final OciLayoutParser ociLayoutParser;

    public OciImageFormatMatchesChecker(OciLayoutParser ociLayoutParser) {
        this.ociLayoutParser = ociLayoutParser;
    }

    @Override
    public boolean applies(File imageDir) throws IntegrationException {
        try {
            logger.debug("Checking to see if this image is an OCI image by checking for file oci-layout");
            File ociLayoutFile = new File(imageDir, "oci-layout");
            if (ociLayoutFile.exists()) {
                String ociLayoutFileContents = FileUtils.readFileToString(ociLayoutFile, StandardCharsets.UTF_8);
                logger.trace("oci-layout file contents: {}", ociLayoutFileContents);
                String ociVersion = ociLayoutParser.parseOciVersion(ociLayoutFileContents);
                logger.info("OCI image format version: {}", ociVersion);
                return true;
            }
        } catch (Exception e) {
            throw new IntegrationException("Image file oci-layer exists but either can't be read or can't be parsed", e);
        }
        return false;
    }
}
