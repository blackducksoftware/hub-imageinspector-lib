/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.none;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.api.PackageManagerEnum;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.ComponentRelationshipPopulater;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrInitializer;
import com.blackduck.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class NullPkgMgr implements PkgMgr {
    @Override
    public boolean isApplicable(final File targetImageFileSystemRootDir) {
        return false;
    }

    @Override
    public PackageManagerEnum getType() {
        return PackageManagerEnum.NULL;
    }

    @Override
    public PkgMgrInitializer getPkgMgrInitializer() {
        return null;
    }

    @Override
    public File getImagePackageManagerDirectory(final File targetImageFileSystemRootDir) {
        return null;
    }

    @Override
    public File getInspectorPackageManagerDirectory() {
        return null;
    }

    @Override
    public List<String> getUpgradeCommand() {
        return null;
    }

    @Override
    public List<String> getListCommand() {
        return null;
    }

    @Override
    public List<ComponentDetails> extractComponentsFromPkgMgrOutput(final File imageFileSystem, final String linuxDistroName, final String[] pkgMgrListOutputLines) throws IntegrationException {
        return new ArrayList<>();
    }

    @Override
    public ComponentRelationshipPopulater createRelationshipPopulator(@Nullable CmdExecutor cmdExecutor) {
        return null;
    }
}
