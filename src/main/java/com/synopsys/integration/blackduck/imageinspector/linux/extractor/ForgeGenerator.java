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

import com.synopsys.integration.hub.bdio.model.Forge;

public class ForgeGenerator {
    public static Forge createProjectForge(final String linuxDistroName) {
        return new Forge("/", "/", linuxDistroName);
    }

    public static Forge createComponentForge(final String linuxDistroName) {
        final String preferredNamespaceForgeId = String.format("@%s", linuxDistroName);
        final Forge preferredNamespaceForge = new Forge("/", "/", preferredNamespaceForgeId);
        return preferredNamespaceForge;
    }
}
