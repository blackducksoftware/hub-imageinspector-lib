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
package com.synopsys.integration.blackduck.imageinspector.api.name;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

public class Names {

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
            appQualifier = "_app";
        }
        if (!StringUtils.isBlank(codelocationPrefix)) {
            return String.format("%s_%s_%s%s_%s", codelocationPrefix, cleanImageName(imageName), imageTag, appQualifier, pkgMgrName);
        }
        return String.format("%s_%s%s_%s", cleanImageName(imageName), imageTag, appQualifier, pkgMgrName);
    }

    private static String slashesToUnderscore(final String givenString) {
        return givenString.replaceAll("/", "_");
    }

    public static String getBlackDuckProjectNameFromImageName(final String imageName) {
        return cleanImageName(imageName);
    }

    private static String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    public static String getContainerFileSystemTarFilename(final String imageNameTag, final String tarPath) {
        final String containerFilesystemFilenameSuffix = "containerfilesystem.tar.gz";
        if (StringUtils.isNotBlank(imageNameTag)) {
            return String.format("%s_%s", cleanImageName(imageNameTag), containerFilesystemFilenameSuffix);
        } else {
            final File tarFile = new File(tarPath);
            final String tarFilename = tarFile.getName();
            if (tarFilename.contains(".")) {
                final int finalPeriodIndex = tarFilename.lastIndexOf('.');
                return String.format("%s_%s", tarFilename.substring(0, finalPeriodIndex), containerFilesystemFilenameSuffix);
            }
            return String.format("%s_%s", cleanImageName(tarFilename), containerFilesystemFilenameSuffix);
        }
    }

    private static String colonsToUnderscores(final String imageName) {
        return imageName.replaceAll(":", "_");
    }
}
