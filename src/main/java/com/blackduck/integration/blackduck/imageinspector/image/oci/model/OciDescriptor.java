/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.annotations.SerializedName;

public class OciDescriptor {
    private static final String REP_TAG_ANNOTATION_KEY = "org.opencontainers.image.ref.name";

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("digest")
    private String digest;

    @SerializedName("size")
    private String size;

    @SerializedName("annotations")
    private Map<String, String> annotations;

    public OciDescriptor(final String mediaType, final String digest, final String size, final Map<String, String> annotations) {
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.annotations = annotations;
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

    // TODO The following methods probably belong in a separate class to keep this class a pure model class

    public Optional<Map<String, String>> getAnnotations() {
        return Optional.ofNullable(annotations);
    }

    public Optional<String> getAnnotation(String key) {
        if (annotations == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(annotations.get(key));
    }

    public Optional<String> getRepoTagString() {
        return getAnnotation(REP_TAG_ANNOTATION_KEY);
    }
}
