/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageConfigParser;

public class OciImageConfigParser implements ImageConfigParser {
    private final CommonImageConfigParser imageConfigParser;

    public OciImageConfigParser(final CommonImageConfigParser imageConfigParser) {
        this.imageConfigParser = imageConfigParser;
    }

    @Override
    public List<String> parseExternalLayerIds(final String imageConfigFileContents) {
        return imageConfigParser.parseExternalLayerIds(imageConfigFileContents);
    }
}
