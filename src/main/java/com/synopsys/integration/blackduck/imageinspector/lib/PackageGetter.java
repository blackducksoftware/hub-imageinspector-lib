/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PackageGetter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PkgMgrExecutor pkgMgrExecutor;
    private final CmdExecutor cmdExecutor;

    @Autowired
    public PackageGetter(PkgMgrExecutor pkgMgrExecutor, CmdExecutor cmdExecutor) {
        this.pkgMgrExecutor = pkgMgrExecutor;
        this.cmdExecutor = cmdExecutor;
    }

    public List<ComponentDetails> queryPkgMgrForDependencies(final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb) {
        final List<ComponentDetails> comps;
        try {
            final String[] pkgMgrOutputLines = pkgMgrExecutor.runPackageManager(cmdExecutor, containerFileSystemWithPkgMgrDb.getPkgMgr(), containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase());
            comps = containerFileSystemWithPkgMgrDb.getPkgMgr().extractComponentsFromPkgMgrOutput(containerFileSystemWithPkgMgrDb.getTargetImageFileSystem().getTargetImageFileSystemFull(), containerFileSystemWithPkgMgrDb.getLinuxDistroName(), pkgMgrOutputLines);
        } catch (IntegrationException e) {
            logger.debug(String.format("Error querying package manager for components: %s", e.getMessage()));
            return new ArrayList<>(0);
        }
        logger.info(String.format("Found %d installed components", comps.size()));
        return comps;
    }
}
