/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.api;

import org.apache.commons.lang3.StringUtils;

public enum ImageInspectorOsEnum {
    UBUNTU,
    CENTOS,
    ALPINE;

    public static ImageInspectorOsEnum determineOperatingSystem(String operatingSystemName) {
        if (StringUtils.isBlank(operatingSystemName)) {
            return null;
        } else {
            operatingSystemName = operatingSystemName.toUpperCase();
            return ImageInspectorOsEnum.valueOf(operatingSystemName);
        }
    }
}
