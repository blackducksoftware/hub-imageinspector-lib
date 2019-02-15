package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrInitializer;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;

public class ApkPkgMgrInitializer implements PkgMgrInitializer {

  @Autowired
  private FileOperations fileOperations;

  @Override
  public void initPkgMgrDir(final File packageManagerDirectory) {
    fileOperations.deleteFilesOnly(packageManagerDirectory);
  }
}
