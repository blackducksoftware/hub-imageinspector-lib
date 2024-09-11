/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk;

import java.io.File;

import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrInitializer;

public class ApkPkgMgrInitializer implements PkgMgrInitializer {
    private FileOperations fileOperations;

    public ApkPkgMgrInitializer(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    @Override
    public void initPkgMgrDir(final File packageManagerDirectory) {
        fileOperations.deleteFilesOnly(packageManagerDirectory);
    }
}
