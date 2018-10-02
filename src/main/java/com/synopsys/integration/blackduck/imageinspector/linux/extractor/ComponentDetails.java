package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

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
