package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.exception.IntegrationException;
import java.io.File;
import java.util.List;

public interface PkgMgr {
  String EXTERNAL_ID_STRING_FORMAT = "%s/%s/%s";

  boolean isApplicable(final File targetImageFileSystemRootDir);
  File getInspectorPackageManagerDirectory();
  ImagePkgMgrDatabase getImagePkgMgrDatabase(final File targetImageFileSystemRootDir);
  List<ComponentDetails> extractComponentsFromPkgMgrOutput(final File imageFileSystem, final String linuxDistroName, final String[] pkgMgrListOutputLines) throws IntegrationException;
}
