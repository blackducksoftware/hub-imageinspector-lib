/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class OciImageIndex {
    @SerializedName("manifests")
    List<OciDescriptor> manifests;

    public OciImageIndex(final List<OciDescriptor> manifests) {
        this.manifests = manifests;
    }

    public List<OciDescriptor> getManifests() {
        return manifests;
    }
}
