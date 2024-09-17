/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.api;

import com.blackduck.integration.util.Stringable;

public class ImageInspectionRequest extends Stringable {
    private final String loggingLevel;
    private final String imageTarfilePath;
    private final String blackDuckProjectName;
    private final String blackDuckProjectVersion;
    private final String codeLocationPrefix;
    private final String givenImageRepo;
    private final String givenImageTag;
    private final boolean organizeComponentsByLayer;
    private final boolean includeRemovedComponents;
    private final boolean cleanupWorkingDir;
    private final String containerFileSystemOutputPath;
    private final String containerFileSystemExcludedPathListString;
    private final String currentLinuxDistro;
    private final String targetLinuxDistroOverride;
    private final String platformTopLayerExternalId;

    public ImageInspectionRequest(final String loggingLevel,
        final String imageTarfilePath,
        final String blackDuckProjectName,
        final String blackDuckProjectVersion,
        final String codeLocationPrefix,
        final String givenImageRepo,
        final String givenImageTag,
        final boolean organizeComponentsByLayer,
        final boolean includeRemovedComponents,
        final boolean cleanupWorkingDir,
        final String containerFileSystemOutputPath,
        final String containerFileSystemExcludedPathListString,
        final String currentLinuxDistro,
        final String targetLinuxDistroOverride,
        final String platformTopLayerExternalId) {
        this.loggingLevel = loggingLevel;
        this.imageTarfilePath = imageTarfilePath;
        this.blackDuckProjectName = blackDuckProjectName;
        this.blackDuckProjectVersion = blackDuckProjectVersion;
        this.codeLocationPrefix = codeLocationPrefix;
        this.givenImageRepo = givenImageRepo;
        this.givenImageTag = givenImageTag;
        this.organizeComponentsByLayer = organizeComponentsByLayer;
        this.includeRemovedComponents = includeRemovedComponents;
        this.cleanupWorkingDir = cleanupWorkingDir;
        this.containerFileSystemOutputPath = containerFileSystemOutputPath;
        this.containerFileSystemExcludedPathListString = containerFileSystemExcludedPathListString;
        this.currentLinuxDistro = currentLinuxDistro;
        this.targetLinuxDistroOverride = targetLinuxDistroOverride;
        this.platformTopLayerExternalId = platformTopLayerExternalId;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public String getImageTarfilePath() {
        return imageTarfilePath;
    }

    public String getBlackDuckProjectName() {
        return blackDuckProjectName;
    }

    public String getBlackDuckProjectVersion() {
        return blackDuckProjectVersion;
    }

    public String getCodeLocationPrefix() {
        return codeLocationPrefix;
    }

    public String getGivenImageRepo() {
        return givenImageRepo;
    }

    public String getGivenImageTag() {
        return givenImageTag;
    }

    public boolean isOrganizeComponentsByLayer() {
        return organizeComponentsByLayer;
    }

    public boolean isIncludeRemovedComponents() {
        return includeRemovedComponents;
    }

    public boolean isCleanupWorkingDir() {
        return cleanupWorkingDir;
    }

    public String getContainerFileSystemOutputPath() {
        return containerFileSystemOutputPath;
    }

    public String getContainerFileSystemExcludedPathListString() {
        return containerFileSystemExcludedPathListString;
    }

    public String getCurrentLinuxDistro() {
        return currentLinuxDistro;
    }

    public String getTargetLinuxDistroOverride() {
        return targetLinuxDistroOverride;
    }

    public String getPlatformTopLayerExternalId() {
        return platformTopLayerExternalId;
    }
}
