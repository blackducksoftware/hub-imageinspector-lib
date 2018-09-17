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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.DpkgExecutor;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;

@Component
public class DpkgExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String PATTERN_FOR_COMPONENT_DETAILS_SEPARATOR = "[  ]+";
    private static final String PATTERN_FOR_LINE_PRECEDING_COMPONENT_LIST = "\\+\\+\\+-=+-=+-=+-=+";

    @Autowired
    private DpkgExecutor executor;

    @Override
    @PostConstruct
    public void init() {
        final List<Forge> forges = new ArrayList<>();
        forges.add(OperatingSystemEnum.UBUNTU.getForge());
        forges.add(OperatingSystemEnum.DEBIAN.getForge());
        initValues(PackageManagerEnum.DPKG, executor, forges);
    }

    @Override
    public String deriveArchitecture(final File targetImageFileSystemRootDir) throws IOException {
        // For dpkg, it's extracted from each component (below)
        return null;
    }

    @Override
    public void extractComponents(final MutableDependencyGraph dependencies, final String dockerImageRepo, final String dockerImageTag, final String givenArchitecture, final String[] packageList, final String preferredAliasNamespace) {
        boolean startOfComponents = false;
        for (final String packageLine : packageList) {

            if (packageLine != null) {
                if (packageLine.matches(PATTERN_FOR_LINE_PRECEDING_COMPONENT_LIST)) {
                    startOfComponents = true;
                } else if (startOfComponents) {
                    // Expect: statusChar name version arch
                    // Or: statusChar name:arch version arch
                    final char packageStatus = packageLine.charAt(1);
                    if (isInstalledStatus(packageStatus)) {
                        final String componentInfo = packageLine.substring(3);
                        final String[] componentInfoParts = componentInfo.trim().split(PATTERN_FOR_COMPONENT_DETAILS_SEPARATOR);
                        String name = componentInfoParts[0];
                        final String version = componentInfoParts[1];
                        final String architecture = componentInfoParts[2];
                        if (name.contains(":")) {
                            name = name.substring(0, name.indexOf(":"));
                        }
                        final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, name, version, architecture);
                        logger.trace(String.format("Constructed externalId: %s", externalId));

                        createBdioComponent(dependencies, name, version, externalId, architecture, preferredAliasNamespace);
                    } else {
                        logger.trace(String.format("Package \"%s\" is listed but not installed (package status: %s)", packageLine, packageStatus));
                    }
                }
            }
        }
    }

    private boolean isInstalledStatus(final Character packageStatus) {
        if (packageStatus == 'i' || packageStatus == 'W' || packageStatus == 't') {
            return true;
        }
        return false;
    }
}
