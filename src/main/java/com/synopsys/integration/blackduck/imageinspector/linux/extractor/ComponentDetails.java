package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import com.synopsys.integration.util.Stringable;

public class ComponentDetails extends Stringable {
    private final String name;
    private final String version;
    private final String externalId;
    private final String architecture;
    private final String preferredAliasNamespace;

    public ComponentDetails(final String name, final String version, final String externalId, final String architecture, final String preferredAliasNamespace) {
        this.name = name;
        this.version = version;
        this.externalId = externalId;
        this.architecture = architecture;
        this.preferredAliasNamespace = preferredAliasNamespace;
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

    public String getPreferredAliasNamespace() {
        return preferredAliasNamespace;
    }
}
