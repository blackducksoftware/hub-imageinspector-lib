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

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.bdio.BdioWriter;
import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.bdio.model.dependency.Dependency;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;

@Component
public class BdioGenerator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SimpleBdioFactory simpleBdioFactory;

    public BdioGenerator() {
        this.simpleBdioFactory = new SimpleBdioFactory();
    }

    public BdioGenerator(final SimpleBdioFactory simpleBdioFactory) {
        this.simpleBdioFactory = simpleBdioFactory;
    }

    public final SimpleBdioDocument generateBdioDocumentFromImageComponentHierarchy(final String codeLocationName, final String projectName,
        final String projectVersion,
        final String linuxDistroName, ImageComponentHierarchy imageComponentHierarchy,
        final boolean showComponentsByLayer,
        final boolean includeRemovedComponents) {

        if (showComponentsByLayer && includeRemovedComponents) {
            return generateTwoLevelBdioDocumentFromHierarchy(codeLocationName, projectName, projectVersion, linuxDistroName, imageComponentHierarchy);
        } else if (showComponentsByLayer && !includeRemovedComponents) {
            // TODO
            throw new UnsupportedOperationException("showComponentsByLayer && !includeRemovedComponents is not yet supported");
        } else if (!showComponentsByLayer && includeRemovedComponents) {
            // TODO
            throw new UnsupportedOperationException("!showComponentsByLayer && includeRemovedComponents is not yet supported");
        } else {
            return generateSingleLevelBdioDocumentFromComponents(codeLocationName, projectName, projectVersion, linuxDistroName, imageComponentHierarchy.getFinalComponents());
        }
    }

    public final SimpleBdioDocument generateSingleLevelBdioDocumentFromComponents(final String codeLocationName, final String projectName,
            final String projectVersion,
            final String linuxDistroName, List<ComponentDetails> comps) {

        final Forge forge = ForgeGenerator.createProjectForge(linuxDistroName);
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(forge, projectName, projectVersion);
        final SimpleBdioDocument bdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersion, projectExternalId);
        final MutableDependencyGraph dependencies = generateSingleLevelDependenciesFromComponents(comps);
        logger.info(String.format("Found %s potential components", dependencies.getRootDependencies().size()));

        simpleBdioFactory.populateComponents(bdioDocument, projectExternalId, dependencies);
        return bdioDocument;
    }

    public final void writeBdio(final File bdioFile, final SimpleBdioDocument bdioDocument) throws IOException {
        simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, bdioDocument);
    }

    public final void writeBdio(final Writer writer, final SimpleBdioDocument bdioDocument) throws IOException {
        try (final BdioWriter bdioWriter = simpleBdioFactory.createBdioWriter(writer)) {
            simpleBdioFactory.writeSimpleBdioDocument(bdioWriter, bdioDocument);
        }
    }

    public String[] getBdioAsStringArray(final SimpleBdioDocument bdioDocument) throws IOException {
        try (final CharArrayWriter charArrayWriter = new CharArrayWriter()) {
            writeBdio(charArrayWriter, bdioDocument);
            final String bdioString = charArrayWriter.toString();
            final String[] bdioLines = bdioString.split("\n");
            return bdioLines;
        }
    }

    private final SimpleBdioDocument generateTwoLevelBdioDocumentFromHierarchy(final String codeLocationName, final String projectName,
        final String projectVersion,
        final String linuxDistroName, final ImageComponentHierarchy imageComponentHierarchy) {

        final Forge forge = ForgeGenerator.createProjectForge(linuxDistroName);
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(forge, projectName, projectVersion);
        final SimpleBdioDocument bdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersion, projectExternalId);
        // TODO This line is the only line that's different in this method, the rest is duplicated; eliminate the duplication
        final MutableDependencyGraph dependencies = generateTwoLevelDependenciesFromHierarchy(imageComponentHierarchy);
        logger.info(String.format("Found %s potential components", dependencies.getRootDependencies().size()));

        simpleBdioFactory.populateComponents(bdioDocument, projectExternalId, dependencies);
        return bdioDocument;
    }

    private MutableDependencyGraph generateTwoLevelDependenciesFromHierarchy(final ImageComponentHierarchy imageComponentHierarchy) {
        // TODO look for a way to reduce duplicated code in this method
        final MutableDependencyGraph graph = simpleBdioFactory.createMutableDependencyGraph();
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            final Forge layerForge = ForgeGenerator.createLayerForge();
            Dependency layerDependency = addDependency(graph, layer.getLayerDotTarDirname(), "none", "none", layerForge, null);
            for (final ComponentDetails comp : layer.getComponents()) {
                final Forge componentForge = ForgeGenerator.createComponentForge(comp.getLinuxDistroName());
                logger.debug(String.format("Generating component with name: %s, version: %s, arch: %s, forge: %s", comp.getName(), comp.getVersion(), comp.getArchitecture(), componentForge.getName()));
                addDependency(graph, comp.getName(), comp.getVersion(), comp.getArchitecture(), componentForge, layerDependency);
            }
        }

        return graph;
    }

    private MutableDependencyGraph generateSingleLevelDependenciesFromComponents(final List<ComponentDetails> comps) {
        final MutableDependencyGraph graph = simpleBdioFactory.createMutableDependencyGraph();
        for (final ComponentDetails comp : comps) {
            final Forge forge = ForgeGenerator.createComponentForge(comp.getLinuxDistroName());
            logger.debug(String.format("Generating component with name: %s, version: %s, arch: %s, forge: %s", comp.getName(), comp.getVersion(), comp.getArchitecture(), forge.getName()));
            addDependency(graph, comp.getName(), comp.getVersion(), comp.getArchitecture(), forge, null);
        }
        return graph;
    }

    private Dependency addDependency(final MutableDependencyGraph graph, final String name, final String version, final String arch, final Forge forge, Dependency parent) {
        final ExternalId extId = simpleBdioFactory.createArchitectureExternalId(forge, name, version, arch);
        final Dependency dep = simpleBdioFactory.createDependency(name, version, extId); // createDependencyNode(forge, name, version, arch);
        logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dep.name, dep.externalId.createBdioId()));
        if (parent == null) {
            graph.addChildToRoot(dep);
        } else {
            graph.addChildWithParent(dep, parent);
        }
        return dep;
    }
}
