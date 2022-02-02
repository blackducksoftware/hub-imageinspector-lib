/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.dpkg;

import java.io.File;
import java.io.IOException;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrInitializer;

public class DpkgPkgMgrInitializer implements PkgMgrInitializer {

    private FileOperations fileOperations;

    public DpkgPkgMgrInitializer(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    @Override
    public void initPkgMgrDir(File packageManagerDatabaseDir) throws IOException {
        fileOperations.deleteFilesOnly(packageManagerDatabaseDir);
        final File statusFile = new File(packageManagerDatabaseDir, "status");
        fileOperations.createNewFile(statusFile);
        final File updatesDir = new File(packageManagerDatabaseDir, "updates");
        fileOperations.mkdir(updatesDir);
    }
}
