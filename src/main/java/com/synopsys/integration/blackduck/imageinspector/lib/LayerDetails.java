package com.synopsys.integration.blackduck.imageinspector.lib;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;

public class LayerDetails {
    private final String layerDotTarDirname;
    private final String layerMetadataFileContents;
    private final List<ComponentDetails> components;

    public LayerDetails(final String layerDotTarDirname, final String layerMetadataFileContents, final List<ComponentDetails> components) {
        this.layerDotTarDirname = layerDotTarDirname;
        this.layerMetadataFileContents = layerMetadataFileContents;
        this.components = components;
    }

    public String getLayerDotTarDirname() {
        return layerDotTarDirname;
    }

    public String getLayerMetadataFileContents() {
        return layerMetadataFileContents;
    }

    public List<ComponentDetails> getComponents() {
        return components;
    }
}
