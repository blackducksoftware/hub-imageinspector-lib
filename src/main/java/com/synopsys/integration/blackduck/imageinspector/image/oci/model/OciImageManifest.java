/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class OciImageManifest {
    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("config")
    private OciDescriptor config;

    @SerializedName("layers")
    private List<OciDescriptor> layers;

    public OciImageManifest(final String mediaType, final OciDescriptor config, final List<OciDescriptor> layers) {
        this.mediaType = mediaType;
        this.config = config;
        this.layers = layers;
    }

    public String getMediaType() {
        return mediaType;
    }

    public OciDescriptor getConfig() {
        return config;
    }

    public List<OciDescriptor> getLayers() {
        return layers;
    }
}
