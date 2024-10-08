/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common.layerentry;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.blackduck.integration.util.Stringable;

public abstract class LayerEntry extends Stringable {
    private List<File> filesAddedByCurrentLayer = new LinkedList<>();

    public Optional<File> process() throws IOException {
        filesAddedByCurrentLayer.addAll(processFiles());
        return fileToDelete();
    }

    // return files added by current layer
    protected abstract List<File> processFiles() throws IOException;
    protected abstract Optional<File> fileToDelete();

    public List<File> getFilesAddedByCurrentLayer() {
        return filesAddedByCurrentLayer;
    }
}
