/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.synopsys.integration.blackduck.imageinspector.lib.components.ImageComponentHierarchy;

public class ContainerFileSystemWithComponents {
    private ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb;
    private ImageComponentHierarchy imageComponentHierarchy;

    public ContainerFileSystemWithComponents(ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, ImageComponentHierarchy imageComponentHierarchy) {
        this.containerFileSystemWithPkgMgrDb = containerFileSystemWithPkgMgrDb;
        this.imageComponentHierarchy = imageComponentHierarchy;
    }

    public ContainerFileSystemWithPkgMgrDb getContainerFileSystemWithPkgMgrDb() {
        return containerFileSystemWithPkgMgrDb;
    }

    public ImageComponentHierarchy getImageComponentHierarchy() {
        return imageComponentHierarchy;
    }
}
