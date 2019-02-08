/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.imageinspector.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;

public enum PackageManagerEnum {
    DPKG("/var/lib/dpkg", OperatingSystemEnum.UBUNTU),
    RPM("/var/lib/rpm", OperatingSystemEnum.CENTOS),
    APK("/lib/apk", OperatingSystemEnum.ALPINE),
    NULL(null, null);

    private static final Logger logger = LoggerFactory.getLogger(PackageManagerEnum.class);
    private final String directory;
    private final OperatingSystemEnum inspectorOperatingSystem;

    private PackageManagerEnum(final String directory, final OperatingSystemEnum inspectorOperatingSystem) {
        this.directory = directory;
        this.inspectorOperatingSystem = inspectorOperatingSystem;
    }

    public static PackageManagerEnum getPackageManagerEnumByName(String name) {
        logger.trace(String.format("Checking to see whether %s is a package manager", name));
        PackageManagerEnum matchingPkgMgr = null;
        if (name != null) {
            name = name.toUpperCase();
            matchingPkgMgr = PackageManagerEnum.valueOf(name);
            logger.trace(String.format("%s matched package manager %s", name, matchingPkgMgr));
        }
        return matchingPkgMgr;
    }

    public String getDirectory() {
        return directory;
    }

    public OperatingSystemEnum getInspectorOperatingSystem() {
        return inspectorOperatingSystem;
    }
}
