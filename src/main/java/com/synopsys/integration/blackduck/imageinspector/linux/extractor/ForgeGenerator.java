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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.bdio.model.Forge;

public class ForgeGenerator {

    // For cases where the KB name does not match the Linux distro ID found in os-release/lsb-release,
    // this table provides the mapping.
    // If the KB name matches the Linux distro ID found in os-release/lsb-release, there is
    // no need to add the distro to this table.
    // Linux distro names are mapped to lowercase before being looked up in this table.
    private static final Map<String, String> linuxDistroNameToKbForgeNameMapping = new HashMap<>();

    static {
        linuxDistroNameToKbForgeNameMapping.put("rhel", "redhat");
        linuxDistroNameToKbForgeNameMapping.put("sles", "opensuse");
    }

    public static Forge createProjectForge(final String linuxDistroName) {
        return createForge(linuxDistroName, false);
    }

    public static Forge createLayerForge() {
        return new Forge("/", "/", "DOCKER_INSPECTOR");
    }

    public static Forge createComponentForge(final String linuxDistroName) {
        return createForge(linuxDistroName, true);
    }

    private static Forge createForge(final String linuxDistroName, boolean doPreferredAliasNamespace) {
        if (StringUtils.isBlank(linuxDistroName)) {
            return new Forge("/", "/", "none");
        }
        final String linuxDistroNameLowerCase = linuxDistroName == null ? "" : linuxDistroName.toLowerCase();
        String kbName = linuxDistroNameLowerCase;
        if (linuxDistroNameToKbForgeNameMapping.containsKey(linuxDistroNameLowerCase)) {
            kbName = linuxDistroNameToKbForgeNameMapping.get(linuxDistroNameLowerCase);
        }
        String forgeId;
        if (doPreferredAliasNamespace) {
            forgeId = String.format("@%s", kbName);
        } else {
            forgeId = kbName;
        }
        final Forge preferredNamespaceForge = new Forge("/", "/", forgeId);
        return preferredNamespaceForge;
    }
}
