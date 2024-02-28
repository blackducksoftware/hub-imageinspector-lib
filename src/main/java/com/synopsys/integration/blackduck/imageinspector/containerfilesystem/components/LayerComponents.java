/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components;

import com.synopsys.integration.blackduck.imageinspector.image.common.LayerMetadata;

import java.util.List;

public class LayerComponents {
    private final LayerMetadata layerMetadata;
    private final List<ComponentDetails> components;

    public LayerComponents(LayerMetadata layerMetadata, List<ComponentDetails> components) {
        this.layerMetadata = layerMetadata;
        this.components = components;
    }

    public LayerMetadata getLayerMetadata() {
        return layerMetadata;
    }

    public List<ComponentDetails> getComponents() {
        return components;
    }
}
