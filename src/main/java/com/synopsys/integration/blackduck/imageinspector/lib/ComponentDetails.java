/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
