/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common.layerentry;

import java.io.File;
import java.util.Optional;

public abstract class LayerEntryNoFileToDelete extends LayerEntry {
    @Override
    protected Optional<File> fileToDelete() {
        return Optional.empty();
    }

}