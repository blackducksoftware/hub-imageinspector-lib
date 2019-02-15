package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import java.io.File;

public interface PkgMgrInitializer {
  void initPkgMgrDir(File packageManagerDatabaseDir);
}
