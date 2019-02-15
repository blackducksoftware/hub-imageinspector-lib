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
package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.exception.IntegrationException;
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

public class ApkComponentExtractor implements ComponentExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final List<String> UPGRADE_DATABASE_COMMAND = null;
    public static final List<String> LIST_COMPONENTS_COMMAND = Arrays.asList("apk", "info", "-v");
    private static final String ARCH_FILENAME = "arch";
    private static final String ETC_SUBDIR_CONTAINING_ARCH = "apk";
    private final FileOperations fileOperations;
    private final PkgMgrExecutor pkgMgrExecutor;
    private final File imageFileSystem;
    private String architecture;
    // TODO TEMP
    private PkgMgr pkgMgr;

    public ApkComponentExtractor(final PkgMgr pkgMgr, final FileOperations fileOperations, final PkgMgrExecutor pkgMgrExecutor, final File imageFileSystem, final String architecture) {
        this.pkgMgr = pkgMgr;
        this.fileOperations = fileOperations;
        this.pkgMgrExecutor = pkgMgrExecutor;
        this.imageFileSystem = imageFileSystem;
        this.architecture = architecture;
    }

    @Override
    public List<ComponentDetails> extractComponents(final ImagePkgMgrDatabase imagePkgMgrDatabase,
            final String linuxDistroName) throws IntegrationException {
        final String[] pkgMgrListOutputLines = pkgMgrExecutor.runPackageManager(pkgMgr, imagePkgMgrDatabase);
        final List<ComponentDetails> components = extractComponentsFromPkgMgrOutput(linuxDistroName, pkgMgrListOutputLines);
        return components;
    }

    @Override
    public List<ComponentDetails> extractComponentsFromPkgMgrOutput(final String linuxDistroName, final String[] pkgMgrListOutputLines) throws IntegrationException {
        final List<ComponentDetails> components = new ArrayList<>();

        for (final String packageLine : pkgMgrListOutputLines) {
            if (!packageLine.toLowerCase().startsWith("warning")) {
                logger.trace(String.format("packageLine: %s", packageLine));
                // Expected format: component-versionpart1-versionpart2
                // component may contain dashes (often contains one).
                final String[] parts = packageLine.split("-");
                if (parts.length < 3) {
                    logger.warn(String.format("apk output contains an invalid line: %s", packageLine));
                    continue;
                }
                final String version = String.format("%s-%s", parts[parts.length - 2], parts[parts.length - 1]);
                logger.trace(String.format("version: %s", version));
                String component = "";
                for (int i = 0; i < parts.length - 2; i++) {
                    final String part = parts[i];
                    if (StringUtils.isNotBlank(component)) {
                        component += String.format("-%s", part);
                    } else {
                        component = part;
                    }
                }
                logger.trace(String.format("component: %s", component));
                // if a package starts with a period, ignore it. It's a virtual meta package and the version information is missing
                if (!component.startsWith(".")) {
                    final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, component, version, getArchitecture());
                    logger.debug(String.format("Constructed externalId: %s", externalId));
                    components.add(new ComponentDetails(component, version, externalId, getArchitecture(), linuxDistroName));
                }
            }
        }
        return components;
    }

    private String getArchitecture() throws IntegrationException {
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
