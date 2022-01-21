/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import com.synopsys.integration.exception.IntegrationException;

public interface PkgMgr {
    boolean isApplicable(final File targetImageFileSystemRootDir);
    PackageManagerEnum getType();
    PkgMgrInitializer getPkgMgrInitializer();
    File getImagePackageManagerDirectory(final File targetImageFileSystemRootDir);
    File getInspectorPackageManagerDirectory();
    List<String> getUpgradeCommand();
    List<String> getListCommand();
    List<ComponentDetails> extractComponentsFromPkgMgrOutput(final File imageFileSystem, final String linuxDistroName, final String[] pkgMgrListOutputLines) throws IntegrationException;
    ComponentRelationshipPopulater createRelationshipPopulator();
    DbRelationshipInfo getRelationshipInfo();

}
