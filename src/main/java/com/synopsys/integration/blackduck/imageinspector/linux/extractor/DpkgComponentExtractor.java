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
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.exception.IntegrationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DpkgComponentExtractor implements ComponentExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final List<String> UPGRADE_DATABASE_COMMAND = null;
    public static final List<String> LIST_COMPONENTS_COMMAND = Arrays.asList("dpkg", "-l");
    private static final String PATTERN_FOR_COMPONENT_DETAILS_SEPARATOR = "[  ]+";
    private static final String PATTERN_FOR_LINE_PRECEDING_COMPONENT_LIST = "\\+\\+\\+-=+-=+-=+-=+";
    private final PkgMgrExecutor pkgMgrExecutor;
    private final PkgMgr pkgMgr;

    public DpkgComponentExtractor(final PkgMgr pkgMgr, final PkgMgrExecutor pkgMgrExecutor) {
        this.pkgMgr = pkgMgr;
        this.pkgMgrExecutor = pkgMgrExecutor;
    }

    @Override
    public List<ComponentDetails> extractComponents(final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName)
            throws IntegrationException {

        final String[] packageList = pkgMgrExecutor.runPackageManager(pkgMgr, imagePkgMgrDatabase);
        final List<ComponentDetails> components = extractComponentsFromPkgMgrOutput(linuxDistroName, packageList);
        return components;
    }

    @Override
    public List<ComponentDetails> extractComponentsFromPkgMgrOutput(final String linuxDistroName, final String[] pkgMgrListOutputLines) {
        final List<ComponentDetails> components = new ArrayList<>();
        boolean startOfComponents = false;
        for (final String packageLine : pkgMgrListOutputLines) {

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
                        final String archFromPkgMgrOutput = componentInfoParts[2];
                        if (name.contains(":")) {
                            name = name.substring(0, name.indexOf(":"));
                        }
                        final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, name, version, archFromPkgMgrOutput);
                        logger.trace(String.format("Constructed externalId: %s", externalId));
                        components.add(new ComponentDetails(name, version, externalId, archFromPkgMgrOutput, linuxDistroName));
                    } else {
                        logger.trace(String.format("Package \"%s\" is listed but not installed (package status: %s)", packageLine, packageStatus));
                    }
                }
            }
        }
        return components;
    }

    private boolean isInstalledStatus(final Character packageStatus) {
        if (packageStatus == 'i' || packageStatus == 'W' || packageStatus == 't') {
            return true;
        }
        return false;
    }
}
