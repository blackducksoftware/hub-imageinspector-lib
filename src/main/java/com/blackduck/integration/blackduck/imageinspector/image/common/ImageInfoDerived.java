/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchy;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;

// Comprehensive info about an image, including
// the harder-to-derive bits
public class ImageInfoDerived {
    private final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb;
    private final ImageComponentHierarchy imageComponentHierarchy;
    private final FullLayerMapping fullLayerMapping; //TODO- does ImageInfoDerived really need a FullLayerMapping?  Or just data it contains?
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
