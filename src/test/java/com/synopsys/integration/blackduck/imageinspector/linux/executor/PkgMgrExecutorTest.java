package com.synopsys.integration.blackduck.imageinspector.linux.executor;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.exception.IntegrationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class PkgMgrExecutorTest {

  @Test
  public void testRunPackageManager() throws IOException, IntegrationException {
    final PkgMgrExecutor pkgMgrExecutor = new TestPkgMgrExecutor();
    pkgMgrExecutor.init();
    pkgMgrExecutor.initPkgMgrDir(new File("src/test/resources"));
    pkgMgrExecutor.initValues(new ArrayList<>(), new ArrayList<>());

    final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(new File("src/test/resources/imageDir/ubuntu/var/lib/dpkg"), PackageManagerEnum.DPKG);
    pkgMgrExecutor.runPackageManager(new DpkgPkgMgr(), imagePkgMgrDatabase);

  }

  private class TestPkgMgrExecutor extends PkgMgrExecutor {

    @Override
    public void init() {

    }

    @Override
    protected void initPkgMgrDir(File packageManagerDirectory) throws IOException {

    }
  }

}
