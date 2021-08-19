/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;

public class LayerDetailsBuilder {
    private int layerIndex;
    private TypedArchiveFile archive;
    private String externalId;
    private List<String> cmd;
    private List<ComponentDetails> comps;

    public LayerDetails build() {
        return new LayerDetails(layerIndex, externalId, cmd, comps);
    }

    public int getLayerIndex() {
        return layerIndex;
    }

    public void setLayerIndex(final int layerIndex) {
        this.layerIndex = layerIndex;
    }

    public TypedArchiveFile getArchive() {
        return archive;
    }

    public void setArchive(final TypedArchiveFile archive) {
        this.archive = archive;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public void setCmd(final List<String> cmd) {
        this.cmd = cmd;
    }

    public List<ComponentDetails> getComps() {
        return comps;
    }

    public void setComps(final List<ComponentDetails> comps) {
        this.comps = comps;
    }
}
