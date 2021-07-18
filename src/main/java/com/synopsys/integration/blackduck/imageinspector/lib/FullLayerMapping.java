/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.util.List;

public class FullLayerMapping {
    private ManifestLayerMapping manifestLayerMapping;
    private List<String> layerExternalIds;

    public FullLayerMapping(ManifestLayerMapping manifestLayerMapping, List<String> layerExternalIds) {
        this.manifestLayerMapping = manifestLayerMapping;
        this.layerExternalIds = layerExternalIds;
    }

    public ManifestLayerMapping getManifestLayerMapping() {
        return manifestLayerMapping;
    }

    public List<String> getLayerExternalIds() {
        return layerExternalIds;
    }

    public String getLayerExternalId(final int layerIndex) {
        if ((layerExternalIds == null) || (layerExternalIds.size() < layerIndex + 1)) {
            return null;
        }
        return layerExternalIds.get(layerIndex);
    }
}
