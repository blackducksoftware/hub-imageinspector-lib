/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.synopsys.integration.blackduck.imageinspector.PackageManagerToImageInspectorOsMapping;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import org.springframework.stereotype.Component;

@Component
public class ContainerFileSystemAnalyzer {

    public void checkInspectorOs(ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb, ImageInspectorOsEnum currentOs) throws WrongInspectorOsException {
        final ImageInspectorOsEnum neededInspectorOs = PackageManagerToImageInspectorOsMapping
                .getImageInspectorOs(containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getPackageManager());
        if (!neededInspectorOs.equals(currentOs)) {
            final String msg = String.format("This image needs to be inspected on %s", neededInspectorOs);
            throw new WrongInspectorOsException(neededInspectorOs, msg);
        }
    }
}
