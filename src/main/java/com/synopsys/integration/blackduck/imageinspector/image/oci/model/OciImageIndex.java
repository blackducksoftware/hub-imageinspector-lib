package com.synopsys.integration.blackduck.imageinspector.image.oci.model;

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
