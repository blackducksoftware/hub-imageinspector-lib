/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import java.util.List;

import com.synopsys.integration.util.Stringable;

public class ManifestLayerMapping extends Stringable {
    private final String imageName;
    private final String tagName;
    private final String pathToConfigFileFromImageRoot;
    private final List<String> layerInternalIds;

    public ManifestLayerMapping(String imageName, String tagName, String pathToConfigFileFromImageRoot, List<String> layerInternalIds) {
        this.imageName = imageName;
        this.tagName = tagName;
        this.pathToConfigFileFromImageRoot = pathToConfigFileFromImageRoot;
        this.layerInternalIds = layerInternalIds;
    }

    public String getImageName() {
        return imageName;
    }

    public String getTagName() {
        return tagName;
    }

    public String getPathToConfigFileFromImageRoot() {
        return pathToConfigFileFromImageRoot;
    }

    public List<String> getLayerInternalIds() {
        return layerInternalIds;
    }
}
