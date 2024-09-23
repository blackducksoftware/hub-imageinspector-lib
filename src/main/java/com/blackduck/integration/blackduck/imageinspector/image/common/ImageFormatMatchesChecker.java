/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import com.blackduck.integration.exception.IntegrationException;

import java.io.File;

public interface ImageFormatMatchesChecker {
    boolean applies(File imageDir) throws IntegrationException;
}
