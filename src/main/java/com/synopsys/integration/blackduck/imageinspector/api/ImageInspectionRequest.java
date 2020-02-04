/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.imageinspector.api;

public class ImageInspectionRequest {
    private final String dockerTarfilePath;
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
    private final String platformTopLayerExternalId;

    public ImageInspectionRequest(final String dockerTarfilePath,
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
        final String platformTopLayerExternalId) {
        this.dockerTarfilePath = dockerTarfilePath;
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
        this.platformTopLayerExternalId = platformTopLayerExternalId;
    }

    public String getDockerTarfilePath() {
        return dockerTarfilePath;
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

    public String getPlatformTopLayerExternalId() {
        return platformTopLayerExternalId;
    }
}
