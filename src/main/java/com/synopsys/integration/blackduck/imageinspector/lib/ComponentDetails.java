/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.synopsys.integration.util.Stringable;

public class ComponentDetails extends Stringable {
    private final String name;
    private final String version;
    private final String externalId;
    private final String architecture;
    private final String linuxDistroName;

    public ComponentDetails(final String name, final String version, final String externalId, final String architecture, final String linuxDistroName) {
        this.name = name;
        this.version = version;
        this.externalId = externalId;
        this.architecture = architecture;
        this.linuxDistroName = linuxDistroName;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getLinuxDistroName() {
        return linuxDistroName;
    }
}
