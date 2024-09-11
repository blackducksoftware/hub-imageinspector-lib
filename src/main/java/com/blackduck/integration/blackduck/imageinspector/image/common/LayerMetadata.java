/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import java.util.List;

//TODO- does this class need to exist?  theoretically there could be more metadata we want to collect down the road?
public class LayerMetadata {
    private final List<String> layerCmd;

    public LayerMetadata(List<String> layerCmd) {
        this.layerCmd = layerCmd;
    }

    public List<String> getLayerCmd() {
        return layerCmd;
    }
}
