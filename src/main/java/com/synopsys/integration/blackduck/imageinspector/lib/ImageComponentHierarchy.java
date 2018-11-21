package com.synopsys.integration.blackduck.imageinspector.lib;

import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;

public class ImageComponentHierarchy {
    private final String manifestFileContents;
    private final String imageConfigFileContents;
    private final List<LayerDetails> layers;
    private List<ComponentDetails> finalComponents;

    public ImageComponentHierarchy(final String manifestFileContents, final String imageConfigFileContents, final List<LayerDetails> layers) {
        this.manifestFileContents = manifestFileContents;
        this.imageConfigFileContents = imageConfigFileContents;
        this.layers = layers;
    }

    public ImageComponentHierarchy(final String manifestFileContents, final String imageConfigFileContents) {
        this.manifestFileContents = manifestFileContents;
        this.imageConfigFileContents = imageConfigFileContents;
        this.layers = new ArrayList<>();
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

    public void setFinalComponents(final List<ComponentDetails> finalComponents) {
        this.finalComponents = finalComponents;
    }
}
