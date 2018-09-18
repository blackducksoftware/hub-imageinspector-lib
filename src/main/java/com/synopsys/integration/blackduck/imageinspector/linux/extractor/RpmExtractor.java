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
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.RpmExecutor;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;

@Component
public class RpmExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String PATTERN_FOR_VALID_PACKAGE_LINE = ".+-.+-.+\\..*";
    private final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
    private final static List<Forge> forges = Arrays.asList(OperatingSystemEnum.CENTOS.getForge(), OperatingSystemEnum.FEDORA.getForge(), OperatingSystemEnum.REDHAT.getForge());

    @Autowired
    private final RpmExecutor executor;

    @Autowired
    public RpmExtractor(final RpmExecutor executor) {
        super(PackageManagerEnum.RPM, executor, forges, new SimpleBdioFactory());
        this.executor = executor;
    }

    @Override
    public String deriveArchitecture(final File targetImageFileSystemRootDir) throws IOException {
        // For rpm, it's extracted from each component (below)
        return null;
    }

    @Override
    public void extractComponents(final MutableDependencyGraph dependencies, final String dockerImageRepo, final String dockerImageTag, final String givenArchitecture, final String[] packageList, final String preferredAliasNamespace) {
        for (final String packageLine : packageList) {
            if (valid(packageLine)) {
                // Expected format: name-versionpart1-versionpart2.arch
                final int lastDotIndex = packageLine.lastIndexOf('.');
                final String arch = packageLine.substring(lastDotIndex + 1);
                final int lastDashIndex = packageLine.lastIndexOf('-');
                final String nameVersion = packageLine.substring(0, lastDashIndex);
                final int secondToLastDashIndex = nameVersion.lastIndexOf('-');
                final String versionRelease = packageLine.substring(secondToLastDashIndex + 1, lastDotIndex);
                final String artifact = packageLine.substring(0, secondToLastDashIndex);
                final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, artifact, versionRelease, arch);
                logger.debug(String.format("Adding externalId %s to components list", externalId));
                createBdioComponent(dependencies, artifact, versionRelease, externalId, arch, preferredAliasNamespace);
            }
        }
    }

    private boolean valid(final String packageLine) {
        return packageLine.matches(PATTERN_FOR_VALID_PACKAGE_LINE);
    }
}
