/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem;

import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.blackduck.integration.util.Stringable;

public class ContainerFileSystemWithPkgMgrDb extends Stringable {
    private final ContainerFileSystem containerFileSystem;
    private final ImagePkgMgrDatabase imagePkgMgrDatabase;
    private final String linuxDistroName;
    private final PkgMgr pkgMgr;

    public ContainerFileSystemWithPkgMgrDb(final ContainerFileSystem containerFileSystem, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName, PkgMgr pkgMgr) {
        this.containerFileSystem = containerFileSystem;
        this.imagePkgMgrDatabase = imagePkgMgrDatabase;
        this.linuxDistroName = linuxDistroName;
        this.pkgMgr = pkgMgr;
    }

    public ContainerFileSystem getContainerFileSystem() {
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
}
