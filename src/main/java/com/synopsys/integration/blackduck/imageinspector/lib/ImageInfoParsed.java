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

// Basic information about an image
public class ImageInfoParsed extends Stringable {
    private final TargetImageFileSystem targetImageFileSystem;
    private final ImagePkgMgrDatabase imagePkgMgrDatabase;
    private final String linuxDistroName;
    private final PkgMgr pkgMgr;
    // TODO not sure this belongs here
    private ImageComponentHierarchy imageComponentHierarchy;

    public ImageInfoParsed(final TargetImageFileSystem targetImageFileSystem, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName, PkgMgr pkgMgr,
                           ImageComponentHierarchy imageComponentHierarchy) {
        this.targetImageFileSystem = targetImageFileSystem;
        this.imagePkgMgrDatabase = imagePkgMgrDatabase;
        this.linuxDistroName = linuxDistroName;
        this.pkgMgr = pkgMgr;
        this.imageComponentHierarchy = imageComponentHierarchy;
    }

    public TargetImageFileSystem getTargetImageFileSystem() {
        return targetImageFileSystem;
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
