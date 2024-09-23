package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.api.PackageManagerEnum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.blackduck.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.blackduck.integration.exception.IntegrationException;

public class PkgMgrExecutorTest {
    private static final File inspectorPkgMgrDir = new File("test/fakePkgMgrDir");
    public static final String TEST_COMPONENT = "testComponent";

    @BeforeAll
    public static void setup() {
        if (!inspectorPkgMgrDir.exists()) {
            inspectorPkgMgrDir.mkdirs();
        }
    }

    @Test
    public void testWithoutUpgrade() throws IntegrationException, InterruptedException {
        PkgMgrExecutor executor = new PkgMgrExecutor();
        final PkgMgr testPkgMgr = new TestPkgMgr(Arrays.asList("echo", TEST_COMPONENT), Arrays.asList("echo", TEST_COMPONENT));
        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(new File("src/test/resources/testApkFileSystem/lib/apk"), PackageManagerEnum.APK);
        final String[] output = executor.runPackageManager(new CmdExecutor(), testPkgMgr, imagePkgMgrDatabase);
        assertEquals(1, output.length);
        assertEquals(TEST_COMPONENT, output[0]);
    }

    @Test
    public void testWithUpgrade() throws IntegrationException, InterruptedException {
        PkgMgrExecutor executor = new PkgMgrExecutor();
        final PkgMgr testPkgMgr = new TestPkgMgr(Arrays.asList("thisisnotavalidcommand"), Arrays.asList("echo", TEST_COMPONENT));
        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(new File("src/test/resources/testApkFileSystem/lib/apk"), PackageManagerEnum.APK);
        final String[] output = executor.runPackageManager(new CmdExecutor(), testPkgMgr, imagePkgMgrDatabase);
        assertEquals(1, output.length);
        assertEquals(TEST_COMPONENT, output[0]);
    }

    private class TestPkgMgr implements PkgMgr {
        private PkgMgrInitializer testPkgMgrInitializer = new TestPkgMgrInitializer();
        private final List<String> firstListPkgsCommand;
        private final List<String> subsequentListPkgsCommand;
        private int listCmdCalledCount = 0;

        private TestPkgMgr(final List<String> firstListPkgsCommand, final List<String> subsequentListPkgsCommand) {
            this.firstListPkgsCommand = firstListPkgsCommand;
            this.subsequentListPkgsCommand = subsequentListPkgsCommand;
        }

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
            return Arrays.asList("echo", "mocking upgrade operation");
        }

        @Override
        public List<String> getListCommand() {
            listCmdCalledCount++;
            if (listCmdCalledCount == 1) {
                return firstListPkgsCommand;
            }
            return subsequentListPkgsCommand;
        }

        @Override
        public List<ComponentDetails> extractComponentsFromPkgMgrOutput(final File imageFileSystem, final String linuxDistroName, final String[] pkgMgrListOutputLines) throws IntegrationException {
            return new ArrayList<>();
        }

        @Override
        public ComponentRelationshipPopulater createRelationshipPopulator(CmdExecutor cmdExecutor) {
            return null;
        }

    }

    private class TestPkgMgrInitializer implements  PkgMgrInitializer {

        @Override
        public void initPkgMgrDir(final File packageManagerDatabaseDir) throws IOException {
            System.out.printf("Mock initializing %s\n", packageManagerDatabaseDir.getAbsolutePath());
        }
    }
}
