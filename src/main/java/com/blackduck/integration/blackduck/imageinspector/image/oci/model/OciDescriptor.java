/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
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

    @SerializedName("platform")
    private Map<String, String> platform;

    public OciDescriptor(final String mediaType, final String digest, final String size, final Map<String, String> annotations, final Map<String, String> platform) {
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.annotations = annotations;
        this.platform = platform;
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

    public Optional<Map<String, String>> getPlatform() {
        return Optional.ofNullable(platform);
    }

    public Optional<String> getRepoTagString() {
        return getAnnotation(REP_TAG_ANNOTATION_KEY);
    }
}
