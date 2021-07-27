/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ImageFormatMatchesChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DockerImageFormatMatchesChecker implements ImageFormatMatchesChecker {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean applies(File imageDir) {
        logger.debug("Checking to see if this image is an OCI image by checking for file oci-layout");
        File manifestFile = new File(imageDir, "manifest.json");
        if (manifestFile.exists()) {
            logger.info("Docker image manifest.json file found");
            return true;
        }
        return false;
    }
}
