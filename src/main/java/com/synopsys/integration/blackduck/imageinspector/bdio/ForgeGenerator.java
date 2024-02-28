/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.bdio;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.bdio.model.Forge;

public class ForgeGenerator {
    private static final String REDHAT_KB_NAME = "redhat";
    private static final String REDHAT_DISTRO_NAME = "rhel";

    private static final String OPENSUSE_KB_NAME = "opensuse";
    private static final String OPENSUSE_DISTRO_NAME1 = "sles";
    private static final String OPENSUSE_DISTRO_NAME2 = OPENSUSE_KB_NAME;

    private static final String AMAZON_KB_NAME = "centos";
    private static final String AMAZON_DISTRO_NAME = "amzn";

    // For cases where the KB name does not match the Linux distro ID found in os-release/lsb-release,
    // this table provides the mapping.
    // If the KB name matches the Linux distro ID found in os-release/lsb-release, there is
    // no need to add the distro to this table.
    // Linux distro names are mapped to lowercase before being looked up in this table.
    // The lookup is a "starts with" comparison, so a key of "opensuse" matches any Linux distro
    // name that starts with "opensuse" (case INsensitive).
    private static final Map<String, String> linuxDistroNameToKbForgeNameMapping = new HashMap<>();

    static {
        linuxDistroNameToKbForgeNameMapping.put(REDHAT_DISTRO_NAME, REDHAT_KB_NAME);
        linuxDistroNameToKbForgeNameMapping.put(OPENSUSE_DISTRO_NAME1, OPENSUSE_KB_NAME);
        linuxDistroNameToKbForgeNameMapping.put(OPENSUSE_DISTRO_NAME2, OPENSUSE_KB_NAME);
        linuxDistroNameToKbForgeNameMapping.put(AMAZON_DISTRO_NAME, AMAZON_KB_NAME);
    }

    private ForgeGenerator() {
    }

    public static Forge createProjectForge(final String linuxDistroName) {
        return createForge(linuxDistroName, false);
    }

    public static Forge createLayerForge() {
        return new Forge("/","DOCKER_INSPECTOR");
    }

    public static Forge createComponentForge(final String linuxDistroName) {
        return createForge(linuxDistroName, true);
    }

    private static Forge createForge(final String linuxDistroName, boolean doPreferredAliasNamespace) {
        if (StringUtils.isBlank(linuxDistroName)) {
            return new Forge("/","none");
        }
        final String linuxDistroNameLowerCase = linuxDistroName.toLowerCase();
        Optional<String> overriddenKbName = findMatch(linuxDistroNameLowerCase);
        String kbName = overriddenKbName.orElse(linuxDistroNameLowerCase);
        return new Forge("/", kbName, doPreferredAliasNamespace);
    }

    private static Optional<String> findMatch(final String linuxDistroNameLowerCase) {
        for (Map.Entry<String, String> mappingEntry : linuxDistroNameToKbForgeNameMapping.entrySet()) {
            if (linuxDistroNameLowerCase.startsWith(mappingEntry.getKey().toLowerCase())) {
                return Optional.of(mappingEntry.getValue());
            }
        }
        return Optional.empty();
    }
}
