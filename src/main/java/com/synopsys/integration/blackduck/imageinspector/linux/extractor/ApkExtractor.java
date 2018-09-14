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
package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ApkExecutor;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;

@Component
public class ApkExtractor extends Extractor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ApkExecutor executor;

    @Override
    @PostConstruct
    public void init() {
        final List<Forge> forges = new ArrayList<>();
        forges.add(OperatingSystemEnum.ALPINE.getForge());
        initValues(PackageManagerEnum.APK, executor, forges);
    }

    // Expected format: component-versionpart1-versionpart2
    // component may contain dashes (often contains one.
    @Override
    public void extractComponents(final MutableDependencyGraph dependencies, final String dockerImageRepo, final String dockerImageTag, final String architecture, final String[] packageList, final String preferredAliasNamespace) {
        for (final String packageLine : packageList) {
            if (!packageLine.toLowerCase().startsWith("warning")) {
                logger.trace(String.format("packageLine: %s", packageLine));
                final String[] parts = packageLine.split("-");
                if (parts.length < 3) {
                    logger.error(String.format("apk output contains an invalid line: %s", packageLine));
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
                // if a package starts with a period, we should ignore it because it is a virtual meta package and the version information is missing
                if (!component.startsWith(".")) {
                    final String externalId = String.format("%s/%s/%s", component, version, architecture);
                    logger.debug(String.format("Constructed externalId: %s", externalId));
                    createBdioComponent(dependencies, component, version, externalId, architecture, preferredAliasNamespace);
                }
            }
        }
    }

    @Override
    public String deriveArchitecture(final File targetImageFileSystemRootDir) throws IOException {
        String architecture = null;
        final Optional<File> etc = new LinuxFileSystem(targetImageFileSystemRootDir).getEtcDir();
        if (etc.isPresent()) {
            final File apkDir = new File(etc.get(), "apk");
            if (apkDir.isDirectory()) {
                final File architectureFile = new File(apkDir, "arch");
                if (architectureFile.isFile()) {
                    architecture = FileUtils.readLines(architectureFile, StandardCharsets.UTF_8).get(0);
                }
            }
        }
        return architecture;
    }
}
