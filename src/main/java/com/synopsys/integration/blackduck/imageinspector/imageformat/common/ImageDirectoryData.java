/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.lib.FullLayerMapping;

import java.util.List;

public class ImageDirectoryData {
    private final String actualRepo;
    private final String actualTag;
    private final FullLayerMapping fullLayerMapping;
    private final List<TypedArchiveFile> orderedLayerArchives;

    public ImageDirectoryData(String actualRepo, String actualTag, FullLayerMapping fullLayerMapping, List<TypedArchiveFile> orderedLayerArchives) {
        this.actualRepo = actualRepo;
        this.actualTag = actualTag;
        this.fullLayerMapping = fullLayerMapping;
        this.orderedLayerArchives = orderedLayerArchives;
    }

    public String getActualRepo() {
        return actualRepo;
    }

    public String getActualTag() {
        return actualTag;
    }

    public FullLayerMapping getFullLayerMapping() {
        return fullLayerMapping;
    }

    public List<TypedArchiveFile> getOrderedLayerArchives() {
        return orderedLayerArchives;
    }
}
