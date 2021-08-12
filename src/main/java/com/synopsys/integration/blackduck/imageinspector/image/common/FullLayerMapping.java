/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class FullLayerMapping {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ManifestLayerMapping manifestLayerMapping;
    private final List<String> layerExternalIds;

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

    // image format independent: Image == DockerImageDirectory
    public Optional<Integer> getPlatformTopLayerIndex(@Nullable String platformTopLayerExternalId) {
        if (platformTopLayerExternalId != null) {
            int curLayerIndex = 0;
            for (String candidateLayerExternalId : getLayerExternalIds()) {
                if ((candidateLayerExternalId != null) && (candidateLayerExternalId.equals(platformTopLayerExternalId))) {
                    logger.trace("Found platform top layer ({}) at layerIndex: {}", platformTopLayerExternalId, curLayerIndex);
                    return Optional.of(curLayerIndex);
                }
                curLayerIndex++;
            }
        }
        return Optional.empty();
    }
}
