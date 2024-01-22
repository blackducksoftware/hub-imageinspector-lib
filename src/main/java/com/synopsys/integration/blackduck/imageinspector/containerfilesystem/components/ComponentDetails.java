/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components;

import java.util.LinkedList;
import java.util.List;

import com.synopsys.integration.util.Stringable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentDetails extends Stringable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final String name;
    private final String version;
    private final String externalId;
    private final String architecture;
    private final String linuxDistroName;
    private List<String> provides;
    private List<ComponentDetails> dependencies;

    public ComponentDetails(final String name, final String version, final String externalId, final String architecture, final String linuxDistroName) {
        this.name = name;
        if (version != null && version.indexOf("0:") == 0) {
            this.version = stripEpocFromVersion(version);
            this.externalId = stripEpochFromExternalId(externalId);
        } else {
            this.version = version;
            this.externalId = externalId;
        }
        logger.debug("Input Version: {}, Final Version: {}", version, this.version);
        logger.debug("Input External Id: {}, Final External Id: {}", externalId, this.externalId);
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

    private String stripEpocFromVersion(String version) {
        return version.substring(2);
    }
    
    private String stripEpochFromExternalId(String externalId) {
        int pos;
        if (externalId != null && (pos = externalId.indexOf("/0:")) > -1) {
            return externalId.substring(0, pos + 1).concat(externalId.substring(pos + 3));
        }
        return externalId;
    }
    
}
