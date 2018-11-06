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

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioWriter;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;

public class BdioGenerator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ComponentExtractor componentExtractor;
    private final SimpleBdioFactory simpleBdioFactory;
    private final ImagePkgMgrDatabase imagePkgMgrDatabase;

    public BdioGenerator(final SimpleBdioFactory simpleBdioFactory, final ComponentExtractor componentExtractor, final ImagePkgMgrDatabase imagePkgMgrDatabase) {
        this.componentExtractor = componentExtractor;
        this.simpleBdioFactory = simpleBdioFactory;
        this.imagePkgMgrDatabase = imagePkgMgrDatabase;
    }

    public final SimpleBdioDocument extract(final String codeLocationName, final String projectName,
            final String projectVersion,
            final String linuxDistroName)
            throws IntegrationException, IOException, InterruptedException {

        final SimpleBdioDocument bdioDocument = extractBdio(codeLocationName, projectName, projectVersion, linuxDistroName);
        return bdioDocument;
    }

    public static final void writeBdio(final BdioWriter bdioWriter, final SimpleBdioDocument bdioDocument) {
        new SimpleBdioFactory().writeSimpleBdioDocument(bdioWriter, bdioDocument);
    }

    private SimpleBdioDocument extractBdio(final String codeLocationName, final String projectName,
            final String version,
            final String linuxDistroName)
            throws IntegrationException, IOException, InterruptedException {
        final Forge forge = ForgeGenerator.createProjectForge(linuxDistroName);
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(forge, projectName, version);
        final SimpleBdioDocument bdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, version, projectExternalId);

        final List<ComponentDetails> comps = componentExtractor.extractComponents(imagePkgMgrDatabase, linuxDistroName);
        final MutableDependencyGraph dependencies = generateDependencies(comps);
        logger.info(String.format("Found %s potential components", dependencies.getRootDependencies().size()));

        simpleBdioFactory.populateComponents(bdioDocument, projectExternalId, dependencies);
        return bdioDocument;
    }

    private MutableDependencyGraph generateDependencies(final List<ComponentDetails> comps) {
        final MutableDependencyGraph dependencies = simpleBdioFactory.createMutableDependencyGraph();
        for (final ComponentDetails comp : comps) {
            final Forge forge = ForgeGenerator.createComponentForge(comp.getLinuxDistroName());
            logger.debug(String.format("Generating component with forge: %s", forge.getName()));
            addDependency(dependencies, comp.getName(), comp.getVersion(), comp.getArchitecture(), forge);
        }
        return dependencies;
    }

    private void addDependency(final MutableDependencyGraph dependencies, final String name, final String version, final String arch, final Forge forge) {
        final ExternalId extId = simpleBdioFactory.createArchitectureExternalId(forge, name, version, arch);
        final Dependency dep = simpleBdioFactory.createDependency(name, version, extId); // createDependencyNode(forge, name, version, arch);
        logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dep.name, dep.externalId.createBdioId()));
        dependencies.addChildToRoot(dep);
    }
}
