/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.synopsys.integration.util.Stringable;

public abstract class LayerEntry extends Stringable {
    public Optional<File> process() throws IOException {
        processFiles();
        return fileToDelete();
    }

    protected abstract void processFiles() throws IOException;
    protected abstract Optional<File> fileToDelete();

}
