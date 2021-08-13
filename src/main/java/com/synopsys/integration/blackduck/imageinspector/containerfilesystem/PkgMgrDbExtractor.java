/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem;

import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO need a test for this

@Component
public class PkgMgrDbExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<PkgMgr> pkgMgrs;
    private final LinuxDistroExtractor linuxDistroExtractor;

    @Autowired
    public PkgMgrDbExtractor(final List<PkgMgr> pkgMgrs, LinuxDistroExtractor linuxDistroExtractor) {
        this.pkgMgrs = pkgMgrs;
        this.linuxDistroExtractor = linuxDistroExtractor;
    }

    public ContainerFileSystemWithPkgMgrDb extract(final ContainerFileSystem containerFileSystem, final String targetLinuxDistroOverride) throws PkgMgrDataNotFoundException {
        if (pkgMgrs == null) {
            logger.error("No pmgMgrs configured");
        } else {
            logger.trace(String.format("pkgMgrs.size(): %d", pkgMgrs.size()));
            for (PkgMgr pkgMgr : pkgMgrs) {
                if (pkgMgr.isApplicable(containerFileSystem.getTargetImageFileSystemFull())) {
                    logger.trace(String.format("Package manager %s applies", pkgMgr.getType().toString()));
                    final ImagePkgMgrDatabase targetImagePkgMgr = new ImagePkgMgrDatabase(pkgMgr.getImagePackageManagerDirectory(containerFileSystem.getTargetImageFileSystemFull()),
                            pkgMgr.getType());
                    final String linuxDistroName;
                    if (StringUtils.isNotBlank(targetLinuxDistroOverride)) {
                        linuxDistroName = targetLinuxDistroOverride;
                        logger.trace(String.format("Target linux distro name overridden by caller to: %s", linuxDistroName));
                    } else {
                        linuxDistroName = linuxDistroExtractor.extract(containerFileSystem.getTargetImageFileSystemFull()).orElse(null);
                        logger.trace(String.format("Target linux distro name derived from image file system: %s", linuxDistroName));
                    }
                    return new ContainerFileSystemWithPkgMgrDb(containerFileSystem, targetImagePkgMgr, linuxDistroName, pkgMgr);
                }
            }
        }
        throw new PkgMgrDataNotFoundException("No package manager database found in this Docker image.");
    }

}
