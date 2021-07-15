/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.util.Stringable;

public class ContainerFileSystemWithPkgMgrDb extends Stringable {
    private final ContainerFileSystem containerFileSystem;
    private final ImagePkgMgrDatabase imagePkgMgrDatabase;
    private final String linuxDistroName;
    private final PkgMgr pkgMgr;
    // TODO There ought to be a separate class that adds this:
    private ImageComponentHierarchy imageComponentHierarchy;

    public ContainerFileSystemWithPkgMgrDb(final ContainerFileSystem containerFileSystem, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName, PkgMgr pkgMgr,
                                           ImageComponentHierarchy imageComponentHierarchy) {
        this.containerFileSystem = containerFileSystem;
        this.imagePkgMgrDatabase = imagePkgMgrDatabase;
        this.linuxDistroName = linuxDistroName;
        this.pkgMgr = pkgMgr;
        this.imageComponentHierarchy = imageComponentHierarchy;
    }

    public ContainerFileSystem getTargetImageFileSystem() {
        return containerFileSystem;
    }

    public ImagePkgMgrDatabase getImagePkgMgrDatabase() {
        return imagePkgMgrDatabase;
    }

    public String getLinuxDistroName() {
        return linuxDistroName;
    }

    public PkgMgr getPkgMgr() {
        return pkgMgr;
    }

    public ImageComponentHierarchy getImageComponentHierarchy() {
        return imageComponentHierarchy;
    }
}
