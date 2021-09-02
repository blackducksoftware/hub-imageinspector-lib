/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci.model;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class OciDescriptor {
    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("digest")
    private String digest;

    @SerializedName("size")
    private String size;

    // annotations looks potentially useful (sometimes has repo:tag), but also problematic: spec says it should be
    // an array of strings, but buildah gives it a single string (non-array) value. I've removed the reference for now.

    public OciDescriptor(final String mediaType, final String digest, final String size) {
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getDigest() {
        return digest;
    }

    public String getSize() {
        return size;
    }
}
