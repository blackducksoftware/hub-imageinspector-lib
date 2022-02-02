/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.ComponentRelationshipPopulater;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            PkgMgr pkgMgr = containerFileSystemWithPkgMgrDb.getPkgMgr();
            final String[] pkgMgrOutputLines = pkgMgrExecutor.runPackageManager(cmdExecutor, pkgMgr, containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase());
            comps = pkgMgr.extractComponentsFromPkgMgrOutput(containerFileSystemWithPkgMgrDb.getContainerFileSystem().getTargetImageFileSystemFull(), containerFileSystemWithPkgMgrDb.getLinuxDistroName(), pkgMgrOutputLines);
            ComponentRelationshipPopulater relationshipPopulater = pkgMgr.createRelationshipPopulator(cmdExecutor);
            relationshipPopulater.populateRelationshipInfo(comps);
        } catch (IntegrationException e) {
            logger.debug(String.format("Error querying package manager for components: %s", e.getMessage()));
            return new ArrayList<>(0);
        }
        logger.info(String.format("Found %d installed components", comps.size()));
        return comps;
    }
}
