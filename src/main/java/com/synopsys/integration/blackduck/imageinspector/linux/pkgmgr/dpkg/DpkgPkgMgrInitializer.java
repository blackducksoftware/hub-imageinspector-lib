package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrInitializer;
import java.io.File;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;

public class DpkgPkgMgrInitializer implements PkgMgrInitializer {

  @Autowired
  private FileOperations fileOperations;

  @Override
  public void initPkgMgrDir(File packageManagerDatabaseDir) throws IOException {
    fileOperations.deleteFilesOnly(packageManagerDatabaseDir);
    final File statusFile = new File(packageManagerDatabaseDir, "status");
    statusFile.createNewFile();
    final File updatesDir = new File(packageManagerDatabaseDir, "updates");
    updatesDir.mkdir();
  }
}
