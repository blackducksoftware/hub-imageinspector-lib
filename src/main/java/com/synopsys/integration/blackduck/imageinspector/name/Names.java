/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.blackduck.imageinspector.name;

import java.io.File;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Names {
    private static final Logger logger = LoggerFactory.getLogger(Names.class);

    public static String getImageTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s.tar", cleanImageName(imageName), tagName);
    }

    public static String getTargetImageFileSystemRootDirName(final String imageName, final String imageTag) {
        return String.format("image_%s_v_%s", cleanImageName(imageName), imageTag);
    }

    public static String getCodeLocationName(final String codelocationPrefix, final String imageName, final String imageTag, final String pkgMgrName) {
        if (!StringUtils.isBlank(codelocationPrefix)) {
            return String.format("%s_%s_%s_%s", codelocationPrefix, cleanImageName(imageName), imageTag, pkgMgrName);
        }
        return String.format("%s_%s_%s", cleanImageName(imageName), imageTag, pkgMgrName);
    }

    private static String slashesToUnderscore(final String givenString) {
        return givenString.replaceAll("/", "_");
    }

    public static String getBdioFilename(final String imageName, final String pkgMgrFilePath, final String blackDuckProjectName, final String blackDuckVersionName) {
        logger.debug(String.format("imageName: %s, pkgMgrFilePath: %s, blackDuckProjectName: %s, blackDuckVersionName: %s", imageName, pkgMgrFilePath, blackDuckProjectName, blackDuckVersionName));
        return createBdioFilename(cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanblackDuckProjectName(blackDuckProjectName), blackDuckVersionName);
    }

    private static String createBdioFilename(final String cleanImageName, final String cleanPkgMgrFilePath, final String cleanblackDuckProjectName, final String blackDuckVersionName) {
        final String[] parts = new String[4];
        parts[0] = cleanImageName;
        parts[1] = cleanPkgMgrFilePath;
        parts[2] = cleanblackDuckProjectName;
        parts[3] = blackDuckVersionName;

        String filename = generateFilename(cleanImageName, cleanPkgMgrFilePath, cleanblackDuckProjectName, blackDuckVersionName);
        for (int i = 0; filename.length() >= 255 && i < 4; i++) {
            parts[i] = DigestUtils.sha1Hex(parts[i]);
            if (parts[i].length() > 15) {
                parts[i] = parts[i].substring(0, 15);
            }

            filename = generateFilename(parts[0], parts[1], parts[2], parts[3]);
        }
        return filename;
    }

    public static String getblackDuckProjectNameFromImageName(final String imageName) {
        return cleanImageName(imageName);
    }

    private static String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private static String cleanblackDuckProjectName(final String blackDuckProjectName) {
        return slashesToUnderscore(blackDuckProjectName);
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

    private static String generateFilename(final String cleanImageName, final String cleanPkgMgrFilePath, final String cleanblackDuckProjectName, final String blackDuckVersionName) {
        return String.format("%s_%s_%s_%s_bdio.jsonld", cleanImageName, cleanPkgMgrFilePath, cleanblackDuckProjectName, blackDuckVersionName);
    }

    private static String cleanPath(final String path) {
        return slashesToUnderscore(path);
    }
}
