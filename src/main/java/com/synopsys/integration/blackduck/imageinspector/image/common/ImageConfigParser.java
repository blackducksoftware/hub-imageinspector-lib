/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import com.google.gson.GsonBuilder;

import java.util.List;

public interface ImageConfigParser {
    List<String> parseExternalLayerIds(final String imageConfigFileContents);
}
