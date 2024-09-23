/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.docker;

import com.blackduck.integration.blackduck.imageinspector.image.common.ImageFormatMatchesChecker;
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
