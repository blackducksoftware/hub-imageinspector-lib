/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.io.File;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.util.Stringable;

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
