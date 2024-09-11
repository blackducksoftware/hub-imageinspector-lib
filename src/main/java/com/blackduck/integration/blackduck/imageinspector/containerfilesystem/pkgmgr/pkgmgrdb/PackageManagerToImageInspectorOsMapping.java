/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb;

import com.blackduck.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.blackduck.integration.blackduck.imageinspector.api.PackageManagerEnum;

public class PackageManagerToImageInspectorOsMapping {

    private PackageManagerToImageInspectorOsMapping() {
    }

    public static ImageInspectorOsEnum getImageInspectorOs(final PackageManagerEnum packageManagerType) {
        switch (packageManagerType) {
            case APK:
                return ImageInspectorOsEnum.ALPINE;
            case DPKG:
                return ImageInspectorOsEnum.UBUNTU;
            case RPM:
                return ImageInspectorOsEnum.CENTOS;
            default:
                return null;
        }
    }
}
