package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import java.io.File;
import java.io.IOException;

public interface PkgMgrInitializer {
  void initPkgMgrDir(File packageManagerDatabaseDir) throws IOException;
}
