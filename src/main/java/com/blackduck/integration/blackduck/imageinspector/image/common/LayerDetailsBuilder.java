/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;

public class LayerDetailsBuilder {
    private final int layerIndex;
    private final TypedArchiveFile archive;
    private final String externalId;

    private List<String> cmd; // collected later

    public LayerDetailsBuilder(final int layerIndex, final TypedArchiveFile archive, final String externalId) {
        this.layerIndex = layerIndex;
        this.archive = archive;
        this.externalId = externalId;
    }

    public LayerDetails build(List<ComponentDetails> comps) {
        return new LayerDetails(layerIndex, externalId, cmd, comps);
    }

    public int getLayerIndex() {
        return layerIndex;
    }

    public TypedArchiveFile getArchive() {
        return archive;
    }

    public String getExternalId() {
        return externalId;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public void setCmd(final List<String> cmd) {
        this.cmd = cmd;
    }
}
