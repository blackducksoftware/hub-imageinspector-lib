/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import org.apache.commons.lang3.StringUtils;

public class LayerDetails {
    private final int layerIndex;
    private final String layerExternalId;
    private final List<String> layerCmd;
    private final List<ComponentDetails> components;

    public LayerDetails(final int layerIndex, final String layerExternalId, final List<String> layerCmd, final List<ComponentDetails> components) {
        this.layerIndex = layerIndex;
        this.layerExternalId = layerExternalId;
        this.layerCmd = layerCmd;
        this.components = components;
    }

    public List<String> getLayerCmd() {
        return layerCmd;
    }

    public List<ComponentDetails> getComponents() {
        return components;
    }

    public String getLayerIndexedName() {
        if (StringUtils.isBlank(layerExternalId)) {
            return String.format("Layer%02d", layerIndex);
        } else {
            return String.format("Layer%02d_%s", layerIndex, layerExternalId.replace(":", "_"));
        }
    }
}
