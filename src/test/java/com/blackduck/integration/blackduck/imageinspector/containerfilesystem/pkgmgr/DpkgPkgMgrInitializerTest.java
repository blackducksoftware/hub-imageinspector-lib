package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.dpkg.DpkgPkgMgrInitializer;

public class DpkgPkgMgrInitializerTest {

    @Test
    public void test() throws IOException {
        final File packageManagerDatabaseDir = new File("test");
        final FileOperations fileOperations = Mockito.mock(FileOperations.class);

        DpkgPkgMgrInitializer initializer = new DpkgPkgMgrInitializer(fileOperations);
        initializer.initPkgMgrDir(packageManagerDatabaseDir);

        final File statusFile = new File(packageManagerDatabaseDir, "status");
        final File updatesDir = new File(packageManagerDatabaseDir, "updates");
        Mockito.verify(fileOperations).deleteFilesOnly(packageManagerDatabaseDir);
        Mockito.verify(fileOperations).createNewFile(statusFile);
        Mockito.verify(fileOperations).mkdir(updatesDir);
    }
}
