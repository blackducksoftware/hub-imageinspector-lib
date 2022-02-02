/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.synopsys.integration.util.Stringable;

public class ManifestLayerMapping extends Stringable {
    @Nullable
    private final String imageName;
    @Nullable
    private final String tagName;
    private final String pathToImageConfigFileFromRoot;
    private final List<String> layerInternalIds;

    public ManifestLayerMapping(@Nullable String imageName, @Nullable String tagName, String pathToImageConfigFileFromRoot, List<String> layerInternalIds) {
        this.imageName = imageName;
        this.tagName = tagName;
        this.pathToImageConfigFileFromRoot = pathToImageConfigFileFromRoot;
        this.layerInternalIds = layerInternalIds;
    }

    public ManifestLayerMapping(String pathToImageConfigFileFromRoot, List<String> layerInternalIds) {
        imageName = null;
        tagName = null;
        this.pathToImageConfigFileFromRoot = pathToImageConfigFileFromRoot;
        this.layerInternalIds = layerInternalIds;
    }

    public Optional<String> getImageName() {
        return Optional.ofNullable(imageName);
    }

    public Optional<String> getTagName() {
        return Optional.ofNullable(tagName);
    }

    public String getPathToImageConfigFileFromRoot() {
        return pathToImageConfigFileFromRoot;
    }

    public List<String> getLayerInternalIds() {
        return layerInternalIds;
    }
}
