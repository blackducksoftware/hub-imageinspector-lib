/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem;

import com.blackduck.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.blackduck.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.PackageManagerToImageInspectorOsMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ContainerFileSystemCompatibilityChecker {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void checkInspectorOs(ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, ImageInspectorOsEnum currentOs) throws WrongInspectorOsException {
        if (containerFileSystemWithPkgMgrDb.getPkgMgr() == null) {
            logger.warn("No linux package manager was found in this image");
            return; // if there's no pkg mgr in target image, any image inspector will do
        }
        final ImageInspectorOsEnum neededInspectorOs = PackageManagerToImageInspectorOsMapping
                .getImageInspectorOs(containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getPackageManager());
        if (!neededInspectorOs.equals(currentOs)) {
            final String msg = String.format("This image needs to be inspected on %s", neededInspectorOs);
            throw new WrongInspectorOsException(neededInspectorOs, msg);
        }
    }
}
