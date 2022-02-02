/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDirectoryData {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String actualRepo;
    private final String actualTag;
    private final FullLayerMapping fullLayerMapping;
    private final List<LayerDetailsBuilder> layers;

    public ImageDirectoryData(final String actualRepo, final String actualTag, final FullLayerMapping fullLayerMapping, final List<LayerDetailsBuilder> layers) {
        this.actualRepo = actualRepo;
        this.actualTag = actualTag;
        this.fullLayerMapping = fullLayerMapping;
        this.layers = layers;
    }

    public String getActualRepo() {
        return actualRepo;
    }

    public String getActualTag() {
        return actualTag;
    }


    public FullLayerMapping getFullLayerMapping() {
        return fullLayerMapping;
    }

    public List<LayerDetailsBuilder> getLayerData() {
        return layers;
    }

    public List<String> getLayerExternalIds() {
        return getLayerData().stream()
                   .sorted(Comparator.comparingInt(LayerDetailsBuilder::getLayerIndex))
                   .map(LayerDetailsBuilder::getExternalId)
                   .collect(Collectors.toList());
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
