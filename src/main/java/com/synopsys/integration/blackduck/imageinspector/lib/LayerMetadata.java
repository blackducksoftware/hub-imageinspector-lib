/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.util.List;

public class LayerMetadata {
    private final String layerExternalId;
    private final List<String> layerCmd;

    public LayerMetadata(String layerExternalId, List<String> layerCmd) {
        this.layerExternalId = layerExternalId;
        this.layerCmd = layerCmd;
    }

    public String getLayerExternalId() {
        return layerExternalId;
    }

    public List<String> getLayerCmd() {
        return layerCmd;
    }
}
