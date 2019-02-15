package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm.RpmPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

public class PkgMgrFactory {
  public static PkgMgr createPkgMgr(final PackageManagerEnum packageManagerType)
      throws IntegrationException {
    switch (packageManagerType) {
      case DPKG: return new DpkgPkgMgr();
      case APK: return new ApkPkgMgr();
      case RPM: return new RpmPkgMgr();
      default:
      throw new IntegrationException(String.format("Unexpected package manager type: %s", packageManagerType.toString()));
    }
  }

}
