/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.util.List;

import com.synopsys.integration.util.Stringable;

// TODO I'm not sure the imageConfigFilename should be here; without it, this is more likely to
// be format independent
public class ManifestLayerMapping extends Stringable {
    private final String imageName;
    private final String tagName;
    private final String imageConfigFilename;
    private final List<String> layerInternalIds;
    private final List<String> layerExternalIds;

    // TODO this looks Docker specific?
    // TODO should be two classes, not one
    public ManifestLayerMapping(final String imageName, final String tagName, final String imageConfigFilename, final List<String> layers) {
        this.imageName = imageName;
        this.tagName = tagName;
        this.imageConfigFilename = imageConfigFilename;
        this.layerInternalIds = layers;
        this.layerExternalIds = null;
    }

    public ManifestLayerMapping(final ManifestLayerMapping partialManifestLayerMapping, final List<String> layerExternalIds) {
        this.imageName = partialManifestLayerMapping.getImageName();
        this.tagName = partialManifestLayerMapping.getTagName();
        this.imageConfigFilename = partialManifestLayerMapping.getImageConfigFilename();
        this.layerInternalIds = partialManifestLayerMapping.getLayerInternalIds();
        this.layerExternalIds = layerExternalIds;
    }

    public String getImageName() {
        return imageName;
    }

    public String getTagName() {
        return tagName;
    }

    public String getImageConfigFilename() {
        return imageConfigFilename;
    }

    public List<String> getLayerInternalIds() {
        return layerInternalIds;
    }

    public String getLayerExternalId(final int layerIndex) {
        if ((layerExternalIds == null) || (layerExternalIds.size() < layerIndex + 1)) {
            return null;
        }
        return layerExternalIds.get(layerIndex);
    }
}
