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
