package com.synopsys.integration.blackduck.imageinspector;

import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;

public class PackageManagerToImageInspectorOsMapping {

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
