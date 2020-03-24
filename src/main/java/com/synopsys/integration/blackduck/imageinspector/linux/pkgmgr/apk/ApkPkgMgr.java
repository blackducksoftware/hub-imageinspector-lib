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
package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrInitializer;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrs;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class ApkPkgMgr implements PkgMgr {
    private static final String STANDARD_PKG_MGR_DIR_PATH = "/lib/apk";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final List<String> UPGRADE_DATABASE_COMMAND = null;
    private static final List<String> LIST_COMPONENTS_COMMAND = Arrays.asList("apk", "info", "-v");
    private static final String ARCH_FILENAME = "arch";
    private static final String ETC_SUBDIR_CONTAINING_ARCH = "apk";
    private final PkgMgrInitializer pkgMgrInitializer;
    private String architecture;
    private final File inspectorPkgMgrDir;
    private FileOperations fileOperations;

    @Autowired
    public ApkPkgMgr(final FileOperations fileOperations) {
        pkgMgrInitializer = new ApkPkgMgrInitializer(fileOperations);
        this.inspectorPkgMgrDir = new File(STANDARD_PKG_MGR_DIR_PATH);
        this.fileOperations = fileOperations;
    }

    public ApkPkgMgr(final FileOperations fileOperations, final String architecture) {
        this(fileOperations);
        this.architecture = architecture;
    }

    @Override
    public boolean isApplicable(File targetImageFileSystemRootDir) {
        final File packageManagerDirectory = getImagePackageManagerDirectory(targetImageFileSystemRootDir);
        final boolean applies = packageManagerDirectory.exists();
        logger.debug(String.format("%s %s", this.getClass().getName(), applies ? "applies" : "does not apply"));
        return applies;
    }

    @Override
    public PackageManagerEnum getType() {
        return PackageManagerEnum.APK;
    }

    @Override
    public PkgMgrInitializer getPkgMgrInitializer() {
        return pkgMgrInitializer;
    }

    @Override
    public File getImagePackageManagerDirectory(final File targetImageFileSystemRootDir) {
        return new File(targetImageFileSystemRootDir, STANDARD_PKG_MGR_DIR_PATH);
    }

    @Override
    public File getInspectorPackageManagerDirectory() {
        return inspectorPkgMgrDir;
    }

    @Override
    public List<String> getUpgradeCommand() {
        return UPGRADE_DATABASE_COMMAND;
    }

    @Override
    public List<String> getListCommand() {
        return LIST_COMPONENTS_COMMAND;
    }

    @Override
    public List<ComponentDetails> extractComponentsFromPkgMgrOutput(final File imageFileSystem, final String linuxDistroName,
        final String[] pkgMgrListOutputLines) throws IntegrationException {
        final List<ComponentDetails> components = new ArrayList<>();
        for (final String packageLine : pkgMgrListOutputLines) {
            if (packageLine.toLowerCase().startsWith("warning")) {
                continue;
            }
            logger.trace(String.format("packageLine: %s", packageLine));
            // Expected format: component-versionpart1-versionpart2
            // component may contain dashes (often contains one).
            final String[] parts = packageLine.split("-");
            if (parts.length < 3) {
                logger.warn(String.format("apk output contains an invalid line: %s", packageLine));
                continue;
            }
            final String version = extractVersion(parts);
            final String component = extractComponent(parts);
            // if a package starts with a period, ignore it. It's a virtual meta package and the version information is missing
            if (component.startsWith(".")) {
                continue;
            }
            final String externalId = String.format(PkgMgrs.EXTERNAL_ID_STRING_FORMAT, component, version, getImageArchitecture(imageFileSystem));
            logger.trace(String.format("Constructed externalId: %s", externalId));
            components.add(new ComponentDetails(component, version, externalId, getImageArchitecture(imageFileSystem), linuxDistroName));
        }
        return components;
    }

    private String extractVersion(final String[] parts) {
        final String version = String.format("%s-%s", parts[parts.length - 2], parts[parts.length - 1]);
        logger.trace(String.format("version: %s", version));
        return version;
    }

    private String extractComponent(final String[] parts) {
        String component = "";
        for (int i = 0; i < parts.length - 2; i++) {
            final String part = parts[i];
            if (StringUtils.isNotBlank(component)) {
                component = String.format("%s-%s", component, part);
            } else {
                component = part;
            }
        }
        logger.trace(String.format("component: %s", component));
        return component;
    }

    private String getImageArchitecture(final File imageFileSystem) throws IntegrationException {
        if (architecture == null) {
            architecture = "";
            final Optional<File> etc = new LinuxFileSystem(imageFileSystem, fileOperations).getEtcDir();
            if (etc.isPresent()) {
                final File apkDir = new File(etc.get(), ETC_SUBDIR_CONTAINING_ARCH);
                if (apkDir.isDirectory()) {
                    final File architectureFile = new File(apkDir, ARCH_FILENAME);
                    if (architectureFile.isFile()) {
                        try {
                            architecture = FileUtils.readLines(architectureFile, StandardCharsets.UTF_8).get(0).trim();
                        } catch (final IOException e) {
                            throw new IntegrationException(String.format("Error deriving architecture; cannot read %s: %s", architectureFile.getAbsolutePath(), e.getMessage()));
                        }
                    }
                }
            }
        }
        return architecture;
    }
}
