/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2020 Synopsys, Inc.
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

public class ImageInspectionRequestBuilder {
    private String loggingLevel;
    private String dockerTarfilePath;
    private String blackDuckProjectName;
    private String blackDuckProjectVersion;
    private String codeLocationPrefix;
    private String givenImageRepo;
    private String givenImageTag;
    private boolean organizeComponentsByLayer;
    private boolean includeRemovedComponents;
    private boolean cleanupWorkingDir;
    private String containerFileSystemOutputPath;
    private String containerFileSystemExcludedPathListString;
    private String currentLinuxDistro;
    private String targetLinuxDistroOverride;
    private String platformTopLayerExternalId;

    public ImageInspectionRequestBuilder() {
    }

    public ImageInspectionRequestBuilder setLoggingLevel(final String loggingLevel) {
        this.loggingLevel = loggingLevel;
        return this;
    }
    public ImageInspectionRequestBuilder setDockerTarfilePath(final String dockerTarfilePath) {
        this.dockerTarfilePath = dockerTarfilePath;
        return this;
    }

    public ImageInspectionRequestBuilder setBlackDuckProjectName(final String blackDuckProjectName) {
        this.blackDuckProjectName = blackDuckProjectName;
        return this;
    }

    public ImageInspectionRequestBuilder setBlackDuckProjectVersion(final String blackDuckProjectVersion) {
        this.blackDuckProjectVersion = blackDuckProjectVersion;
        return this;
    }

    public ImageInspectionRequestBuilder setCodeLocationPrefix(final String codeLocationPrefix) {
        this.codeLocationPrefix = codeLocationPrefix;
        return this;
    }

    public ImageInspectionRequestBuilder setGivenImageRepo(final String givenImageRepo) {
        this.givenImageRepo = givenImageRepo;
        return this;
    }

    public ImageInspectionRequestBuilder setGivenImageTag(final String givenImageTag) {
        this.givenImageTag = givenImageTag;
        return this;
    }

    public ImageInspectionRequestBuilder setOrganizeComponentsByLayer(final boolean organizeComponentsByLayer) {
        this.organizeComponentsByLayer = organizeComponentsByLayer;
        return this;
    }

    public ImageInspectionRequestBuilder setIncludeRemovedComponents(final boolean includeRemovedComponents) {
        this.includeRemovedComponents = includeRemovedComponents;
        return this;
    }

    public ImageInspectionRequestBuilder setCleanupWorkingDir(final boolean cleanupWorkingDir) {
        this.cleanupWorkingDir = cleanupWorkingDir;
        return this;
    }

    public ImageInspectionRequestBuilder setContainerFileSystemOutputPath(final String containerFileSystemOutputPath) {
        this.containerFileSystemOutputPath = containerFileSystemOutputPath;
        return this;
    }

    public ImageInspectionRequestBuilder setContainerFileSystemExcludedPathListString(final String containerFileSystemExcludedPathListString) {
        this.containerFileSystemExcludedPathListString = containerFileSystemExcludedPathListString;
        return this;
    }

    public ImageInspectionRequestBuilder setCurrentLinuxDistro(final String currentLinuxDistro) {
        this.currentLinuxDistro = currentLinuxDistro;
        return this;
    }

    public ImageInspectionRequestBuilder setTargetLinuxDistroOverride(final String targetLinuxDistroOverride) {
        this.targetLinuxDistroOverride = targetLinuxDistroOverride;
        return this;
    }

    public ImageInspectionRequestBuilder setPlatformTopLayerExternalId(final String platformTopLayerExternalId) {
        this.platformTopLayerExternalId = platformTopLayerExternalId;
        return this;
    }

    public ImageInspectionRequest build() {
        return new ImageInspectionRequest(
            loggingLevel,
            dockerTarfilePath,
            blackDuckProjectName,
            blackDuckProjectVersion,
            codeLocationPrefix,
            givenImageRepo,
            givenImageTag,
            organizeComponentsByLayer,
            includeRemovedComponents,
            cleanupWorkingDir,
            containerFileSystemOutputPath,
            containerFileSystemExcludedPathListString,
            currentLinuxDistro,
            targetLinuxDistroOverride,
            platformTopLayerExternalId);
    }
}
