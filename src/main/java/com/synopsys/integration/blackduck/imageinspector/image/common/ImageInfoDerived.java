/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchy;

// Comprehensive info about an image, including
// the harder-to-derive bits
public class ImageInfoDerived {
    private final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb;
    private final ImageComponentHierarchy imageComponentHierarchy;
    private final FullLayerMapping fullLayerMapping;
    private final String codeLocationName;
    private final String finalProjectName;
    private final String finalProjectVersionName;
    private final SimpleBdioDocument bdioDocument;

    public ImageInfoDerived(FullLayerMapping fullLayerMapping, ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, ImageComponentHierarchy imageComponentHierarchy,
                            String codeLocationName, String finalProjectName, String finalProjectVersionName, SimpleBdioDocument bdioDocument) {
        this.fullLayerMapping = fullLayerMapping;
        this.containerFileSystemWithPkgMgrDb = containerFileSystemWithPkgMgrDb;
        this.imageComponentHierarchy = imageComponentHierarchy;
        this.codeLocationName = codeLocationName;
        this.finalProjectName = finalProjectName;
        this.finalProjectVersionName = finalProjectVersionName;
        this.bdioDocument = bdioDocument;
    }

    public FullLayerMapping getFullLayerMapping() {
        return fullLayerMapping;
    }

    public ContainerFileSystemWithPkgMgrDb getImageInfoParsed() {
        return containerFileSystemWithPkgMgrDb;
    }

    public ImageComponentHierarchy getImageComponentHierarchy() {
        return this.imageComponentHierarchy;
    }

    public String getCodeLocationName() {
        return codeLocationName;
    }

    public String getFinalProjectName() {
        return finalProjectName;
    }

    public String getFinalProjectVersionName() {
        return finalProjectVersionName;
    }

    public SimpleBdioDocument getBdioDocument() {
        return bdioDocument;
    }
}
