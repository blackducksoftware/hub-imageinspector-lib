package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.Executor;
import com.synopsys.integration.blackduck.imageinspector.linux.extraction.ComponentDetails;
import com.synopsys.integration.exception.IntegrationException;

public class PkgMgrExecutorTest {
    private static final File inspectorPkgMgrDir = new File("test/fakePkgMgrDir");

    @BeforeAll
    public static void setup() {
        if (!inspectorPkgMgrDir.exists()) {
            inspectorPkgMgrDir.mkdirs();
        }
    }

    @Test
    public void test() throws IntegrationException {
        PkgMgrExecutor executor = new PkgMgrExecutor();
        final PkgMgr testPkgMgr = new TestPkgMgr();
        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(new File("src/test/resources/testApkFileSystem/lib/apk"), PackageManagerEnum.APK);
        executor.runPackageManager(new Executor(), testPkgMgr, imagePkgMgrDatabase);
    }

    private class TestPkgMgr implements PkgMgr {
        private PkgMgrInitializer testPkgMgrInitializer = new TestPkgMgrInitializer();

        @Override
        public boolean isApplicable(final File targetImageFileSystemRootDir) {
            return true;
        }

        @Override
        public PackageManagerEnum getType() {
            return PackageManagerEnum.APK;
        }

        @Override
        public PkgMgrInitializer getPkgMgrInitializer() {
            return testPkgMgrInitializer;
        }

        @Override
        public File getImagePackageManagerDirectory(final File targetImageFileSystemRootDir) {
            return new File("src/test/resources/testApkFileSystem/lib/apk");
        }

        @Override
        public File getInspectorPackageManagerDirectory() {
            return new File("test/fakePkgMgrDir");
        }

        @Override
        public List<String> getUpgradeCommand() {
            return Arrays.asList("echo", "");
        }

        @Override
        public List<String> getListCommand() {
            return Arrays.asList("echo", "");
        }

        @Override
        public List<ComponentDetails> extractComponentsFromPkgMgrOutput(final File imageFileSystem, final String linuxDistroName, final String[] pkgMgrListOutputLines) throws IntegrationException {
            return new ArrayList<>();
        }
    }

    private class TestPkgMgrInitializer implements  PkgMgrInitializer {

        @Override
        public void initPkgMgrDir(final File packageManagerDatabaseDir) throws IOException {
            System.out.printf("Mock initializing %s\n", packageManagerDatabaseDir.getAbsolutePath());
        }
    }
}
