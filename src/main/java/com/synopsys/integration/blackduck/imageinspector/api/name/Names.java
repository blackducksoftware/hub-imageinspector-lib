/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.api.name;

import org.apache.commons.lang3.StringUtils;

public class Names {
    private static final String APP_ONLY_HINT = "app";

    private Names() {
    }

    public static String getTargetImageFileSystemRootDirName(final String imageName, final String imageTag) {
        return String.format("image_%s_v_%s", cleanImageName(imageName), imageTag);
    }

    public static String getTargetImageFileSystemAppLayersRootDirName(final String imageName, final String imageTag) {
        return String.format("image_app_layers_%s_v_%s", cleanImageName(imageName), imageTag);
    }

    public static String getCodeLocationName(final String codelocationPrefix, final String imageName, final String imageTag, final String pkgMgrName,
        final boolean platformComponentsExcluded) {
        String appQualifier = "";
        if (platformComponentsExcluded) {
            appQualifier = String.format("_%s", APP_ONLY_HINT);
        }
        if (!StringUtils.isBlank(codelocationPrefix)) {
            return String.format("%s_%s_%s%s_%s", codelocationPrefix, cleanImageName(imageName), imageTag, appQualifier, pkgMgrName);
        }
        return String.format("%s_%s%s_%s", cleanImageName(imageName), imageTag, appQualifier, pkgMgrName);
    }

    public static String getBlackDuckProjectNameFromImageName(String imageName,
        final boolean platformComponentsExcluded) {
        if (platformComponentsExcluded) {
            imageName = String.format("%s_%s", imageName, APP_ONLY_HINT);
        }
        return cleanImageName(imageName);
    }

    private static String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private static String slashesToUnderscore(final String givenString) {
        return givenString.replace("/", "_");
    }

    private static String colonsToUnderscores(final String imageName) {
        return imageName.replace(":", "_");
    }
}
