package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm;

import java.io.File;
import java.io.IOException;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrInitializer;

public class RpmPkgMgrInitializer implements PkgMgrInitializer {

    private FileOperations fileOperations;

    public RpmPkgMgrInitializer(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    @Override
    public void initPkgMgrDir(File packageManagerDatabaseDir) throws IOException {
        fileOperations.deleteFilesOnly(packageManagerDatabaseDir);
    }
}
