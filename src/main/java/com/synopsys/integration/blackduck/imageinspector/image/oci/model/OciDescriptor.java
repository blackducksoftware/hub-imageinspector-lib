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

    @SerializedName("urls")
    private List<String> urls;

    @SerializedName("annotations")
    private List<String> annotations;

    public OciDescriptor(final String mediaType, final String digest, final String size) {
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.urls = new LinkedList<>();
        this.annotations = new LinkedList<>();
    }

    public OciDescriptor(final String mediaType, final String digest, final String size, final List<String> urls, final List<String> annotations) {
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.urls = urls;
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

    public List<String> getUrls() {
        return urls;
    }

    public List<String> getAnnotations() {
        return annotations;
    }
}
