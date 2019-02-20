package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk;

import java.io.File;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrInitializer;

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
