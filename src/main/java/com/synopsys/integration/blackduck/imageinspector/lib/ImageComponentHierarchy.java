/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
