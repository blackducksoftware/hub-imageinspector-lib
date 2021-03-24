/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.util.ArrayList;
import java.util.List;

public class ImageComponentHierarchy {
    private final String manifestFileContents;
    private final String imageConfigFileContents;
    private final List<LayerDetails> layers;
    private int platformTopLayerIndex;
    private List<ComponentDetails> platformComponents;
    private List<ComponentDetails> finalComponents;

    public ImageComponentHierarchy(final String manifestFileContents, final String imageConfigFileContents) {
        this.manifestFileContents = manifestFileContents;
        this.imageConfigFileContents = imageConfigFileContents;
        this.layers = new ArrayList<>();
        platformTopLayerIndex = -1;
        this.platformComponents = new ArrayList<>();
        this.finalComponents = new ArrayList<>();
    }

    public void addLayer(final LayerDetails layer) {
        layers.add(layer);
    }

    public String getManifestFileContents() {
        return manifestFileContents;
    }

    public String getImageConfigFileContents() {
        return imageConfigFileContents;
    }

    public List<LayerDetails> getLayers() {
        return layers;
    }

    public void setPlatformTopLayerIndex(final int platformTopLayerIndex) {
        this.platformTopLayerIndex = platformTopLayerIndex;
    }

    public boolean isPlatformTopLayerFound() {
        return (platformTopLayerIndex >= 0);
    }

    public void setPlatformComponents(final List<ComponentDetails> platformComponents) {
        if (platformComponents == null) {
            return;
        }
        this.platformComponents = platformComponents;
    }

    public void setFinalComponents(final List<ComponentDetails> finalComponents) {
        if (finalComponents == null) {
            return;
        }
        this.finalComponents = finalComponents;
    }

    public List<ComponentDetails> getPlatformComponents() {
        return platformComponents;
    }

    public List<ComponentDetails> getFinalComponents() {
        return finalComponents;
    }
}
