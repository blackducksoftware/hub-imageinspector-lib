/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.api.name;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class Names {
    private static final String APP_ONLY_HINT = "app";
    public static final String UNKNOWN = "unknown";

    private Names() {
    }

    public static String getTargetImageFileSystemRootDirName(final String imageName, final String imageTag) {
        return String.format("image_%s_v_%s", cleanImageName(imageName), imageTag);
    }

    public static String getTargetImageFileSystemAppLayersRootDirName(final String imageName, final String imageTag) {
        return String.format("image_app_layers_%s_v_%s", cleanImageName(imageName), imageTag);
    }

    public static String getCodeLocationName(final String codelocationPrefix, @Nullable String imageName, @Nullable String imageTag, @Nullable String archiveFilename,
                                             final String pkgMgrName, final boolean platformComponentsExcluded) {
        String appQualifier = "";
        if (platformComponentsExcluded) {
            appQualifier = String.format("_%s", APP_ONLY_HINT);
        }
        String effectiveImageName = imageName;
        if (StringUtils.isBlank(effectiveImageName)) {
            effectiveImageName = deriveNameFromFilename(archiveFilename);
        }
        String effectiveTagName = imageTag;
        if (StringUtils.isBlank(effectiveTagName)) {
            effectiveTagName = deriveExtensionFromFilename(archiveFilename);
        }
        if (!StringUtils.isBlank(codelocationPrefix)) {
            return String.format("%s_%s_%s%s_%s", codelocationPrefix, cleanImageName(effectiveImageName), effectiveTagName, appQualifier, pkgMgrName);
        }
        return String.format("%s_%s%s_%s", cleanImageName(effectiveImageName), effectiveTagName, appQualifier, pkgMgrName);
    }

    public static String getBlackDuckProjectNameFromImageName(@Nullable String imageName, @Nullable String archiveFilename,
        final boolean platformComponentsExcluded) {
        String effectiveImageName = imageName;
        if (StringUtils.isBlank(effectiveImageName)) {
            effectiveImageName = deriveNameFromFilename(archiveFilename);
        }
        if (platformComponentsExcluded) {
            effectiveImageName = String.format("%s_%s", effectiveImageName, APP_ONLY_HINT);
        }
        return cleanImageName(effectiveImageName);
    }

    public static String getBlackDuckProjectVersionNameFromImageTag(@Nullable String tagName) {
        if (StringUtils.isBlank(tagName)) {
            return UNKNOWN;
        }
        return tagName;
    }

    private static String deriveNameFromFilename(@Nullable String fileName) {
        String derivedName;
        if (StringUtils.isBlank(fileName) || ".".equals(fileName)) {
            derivedName = UNKNOWN;
        } else {
            if (fileName.startsWith(".")) {
                fileName = fileName.substring(1);
            }
            if (fileName.contains(".")) {
                derivedName = fileName.substring(0, fileName.lastIndexOf('.'));
            } else {
                derivedName = fileName;
            }
        }
        return derivedName;
    }

    private static String deriveExtensionFromFilename(@Nullable String fileName) {
        String derivedExtension;
        if (StringUtils.isBlank(fileName) || ".".equals(fileName)) {
            derivedExtension = UNKNOWN;
        } else {
            if (fileName.startsWith(".")) {
                fileName = fileName.substring(1);
            }
            if (fileName.contains(".") && (!fileName.endsWith("."))) {
                derivedExtension = fileName.substring(fileName.lastIndexOf('.')+1);
            } else {
                derivedExtension = UNKNOWN;
            }
        }
        return derivedExtension;
    }

    private static String cleanImageName(String imageName) {
        if (imageName == null) {
            imageName = "";
        }
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private static String slashesToUnderscore(final String givenString) {
        return givenString.replace("/", "_");
    }

    private static String colonsToUnderscores(final String imageName) {
        return imageName.replace(":", "_");
    }
}
