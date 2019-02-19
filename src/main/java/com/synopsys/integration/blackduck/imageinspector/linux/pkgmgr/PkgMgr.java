package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import java.io.File;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.exception.IntegrationException;

public interface PkgMgr {
    String EXTERNAL_ID_STRING_FORMAT = "%s/%s/%s";

    boolean isApplicable(final File targetImageFileSystemRootDir);
    PackageManagerEnum getType();
    PkgMgrInitializer getPkgMgrInitializer();
    File getImagePackageManagerDirectory(final File targetImageFileSystemRootDir);
    File getInspectorPackageManagerDirectory();
    List<ComponentDetails> extractComponentsFromPkgMgrOutput(final File imageFileSystem, final String linuxDistroName, final String[] pkgMgrListOutputLines) throws IntegrationException;
}
