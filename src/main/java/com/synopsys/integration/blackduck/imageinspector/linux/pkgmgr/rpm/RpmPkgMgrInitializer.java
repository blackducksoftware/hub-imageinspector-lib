package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrInitializer;

public class RpmPkgMgrInitializer implements PkgMgrInitializer {

    @Autowired
    private FileOperations fileOperations;

    @Override
    public void initPkgMgrDir(File packageManagerDatabaseDir) throws IOException {
        fileOperations.deleteFilesOnly(packageManagerDatabaseDir);
    }
}
