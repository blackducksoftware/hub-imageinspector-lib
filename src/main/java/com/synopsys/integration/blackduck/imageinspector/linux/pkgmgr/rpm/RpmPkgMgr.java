package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.exception.IntegrationException;
import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpmPkgMgr implements PkgMgr {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String PKG_MGR_DIR = "/var/lib/rpm";

  @Override
  public boolean isApplicable(File targetImageFileSystemRootDir) {
    final File packageManagerDirectory = getExtractedPackageManagerDirectory(targetImageFileSystemRootDir);
    final boolean applies = packageManagerDirectory.exists();
    logger.debug(String.format("%s %s", this.getClass().getName(), applies ? "applies" : "does not apply"));
    return applies;
  }

  // TODO methods above and below are basically duplicated across all pkg mgrs
  @Override
  public ImagePkgMgrDatabase getImagePkgMgrDatabase(File targetImageFileSystemRootDir) {
    final File inspectorPackageManagerDirectory = new File(PKG_MGR_DIR);
    final File extractedPackageManagerDirectory = getExtractedPackageManagerDirectory(targetImageFileSystemRootDir);
    final ImagePkgMgrDatabase targetImagePkgMgr = new ImagePkgMgrDatabase(inspectorPackageManagerDirectory, extractedPackageManagerDirectory,
        PackageManagerEnum.RPM);
    return targetImagePkgMgr;
  }

  @Override
  public List<ComponentDetails> extractComponentsFromPkgMgrOutput(File imageFileSystem,
      String linuxDistroName, String[] pkgMgrListOutputLines)
      throws IntegrationException {
    return null;
  }

  private File getExtractedPackageManagerDirectory(File targetImageFileSystemRootDir) {
    return new File(targetImageFileSystemRootDir, PKG_MGR_DIR);
  }
}
