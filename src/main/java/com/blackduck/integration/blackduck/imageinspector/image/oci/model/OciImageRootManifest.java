/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class OciImageRootManifest {

    @SerializedName("config")
    private String config;

    @SerializedName("layers")
    private List<OciDescriptor> layers;

    public OciImageRootManifest(final String config, final List<OciDescriptor> layers) {
        this.config = config;
        this.layers = layers;
    }

    public String getConfig() {
        return config;
    }

    public List<OciDescriptor> getLayers() {
        return layers;
    }
}
