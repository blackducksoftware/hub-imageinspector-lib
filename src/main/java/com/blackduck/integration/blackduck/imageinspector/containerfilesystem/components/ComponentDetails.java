/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components;

import java.util.LinkedList;
import java.util.List;

import com.synopsys.integration.util.Stringable;

public class ComponentDetails extends Stringable {
    private final String name;
    private final String version;
    private final String externalId;
    private final String architecture;
    private final String linuxDistroName;
    private List<String> provides;
    private List<ComponentDetails> dependencies;

    public ComponentDetails(final String name, final String version, final String externalId, final String architecture, final String linuxDistroName) {
        this.name = name;
        this.version = version;
        this.externalId = externalId;
        this.architecture = architecture;
        this.linuxDistroName = linuxDistroName;
        this.dependencies = new LinkedList<>();
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

    public List<String> getProvides() { return provides; }

    public void setProvides(final List<String> provides) { this.provides = provides;}

    public List<ComponentDetails> getDependencies() { return dependencies; }

    public void setDependencies(final List<ComponentDetails> dependencies) { this.dependencies = dependencies; }
}
