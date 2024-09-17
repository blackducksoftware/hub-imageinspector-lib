/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb;

import java.io.File;

import com.blackduck.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.blackduck.integration.util.Stringable;

public class ImagePkgMgrDatabase extends Stringable {
    private final File extractedPackageManagerDirectory;
    private final PackageManagerEnum packageManager;

    public ImagePkgMgrDatabase(final File extractedPackageManagerDirectory, final PackageManagerEnum packageManager) {
        this.extractedPackageManagerDirectory = extractedPackageManagerDirectory;
        this.packageManager = packageManager;
    }

    public File getExtractedPackageManagerDirectory() {
        return extractedPackageManagerDirectory;
    }

    public PackageManagerEnum getPackageManager() {
        return packageManager;
    }
}
