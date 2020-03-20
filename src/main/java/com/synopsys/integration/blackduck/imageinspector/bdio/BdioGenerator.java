/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.imageinspector.bdio;

import java.io.CharArrayWriter;
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
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
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

    public SimpleBdioDocument generateBdioDocumentFromImageComponentHierarchy(final String codeLocationName, final String projectName,
        final String projectVersion,
        final String linuxDistroName, ImageComponentHierarchy imageComponentHierarchy,
        final boolean organizeComponentsByLayer,
        final boolean includeRemovedComponents) {

        if (organizeComponentsByLayer) {
            final MutableDependencyGraph graph = generateLayeredGraphFromHierarchy(imageComponentHierarchy, includeRemovedComponents);
            return generateBdioDocumentFromGraph(codeLocationName, projectName, projectVersion, linuxDistroName, graph);
        } else {
            if (includeRemovedComponents) {
                final MutableDependencyGraph graph = generateFlatGraphFromAllComponentsAllLayers(imageComponentHierarchy);
                return generateBdioDocumentFromGraph(codeLocationName, projectName, projectVersion, linuxDistroName, graph);
            } else {
                return generateFlatBdioDocumentFromComponents(codeLocationName, projectName, projectVersion, linuxDistroName, imageComponentHierarchy.getFinalComponents());
            }
        }
    }

    public SimpleBdioDocument generateFlatBdioDocumentFromComponents(final String codeLocationName, final String projectName,
        final String projectVersion,
        final String linuxDistroName, List<ComponentDetails> comps) {
        final MutableDependencyGraph graph = generateFlatGraphFromComponents(comps);
        return generateBdioDocumentFromGraph(codeLocationName, projectName, projectVersion, linuxDistroName, graph);
    }

    public void writeBdio(final Writer writer, final SimpleBdioDocument bdioDocument) throws IOException {
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

    private final SimpleBdioDocument generateBdioDocumentFromGraph(final String codeLocationName, final String projectName,
        final String projectVersion,
        final String linuxDistroName, final MutableDependencyGraph graph) {

        final Forge forge = ForgeGenerator.createProjectForge(linuxDistroName);
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(forge, projectName, projectVersion);
        final SimpleBdioDocument bdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersion, projectExternalId);
        logger.info(String.format("Returning %d components", graph.getRootDependencies().size()));
        simpleBdioFactory.populateComponents(bdioDocument, projectExternalId, graph);
        return bdioDocument;
    }

    private MutableDependencyGraph generateLayeredGraphFromHierarchy(final ImageComponentHierarchy imageComponentHierarchy, final boolean includeRemovedComponents) {
        final MutableDependencyGraph graph = simpleBdioFactory.createMutableDependencyGraph();
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            Dependency layerDependency = addLayerDependency(graph, layer.getLayerIndexedName());
            logger.trace(String.format("Created layer node: %s", layerDependency.getName()));
            for (final ComponentDetails comp : layer.getComponents()) {
                if (imageComponentHierarchy.getFinalComponents().contains(comp)) {
                    logger.trace(String.format("layer comp %s:%s is in final components list; including it in this layer", comp.getName(), comp.getVersion()));
                    addDependency(graph, layerDependency, comp);
                } else {
                    logger.trace(String.format("layer comp %s:%s is not in final component list", comp.getName(), comp.getVersion()));
                    if (includeRemovedComponents) {
                        logger.trace(String.format("\tIncluding it in this layer"));
                        addDependency(graph, layerDependency, comp);
                    } else {
                        logger.trace(String.format("\tExcluding it from this layer"));
                    }
                }
            }
        }
        return graph;
    }

    private MutableDependencyGraph generateFlatGraphFromComponents(final List<ComponentDetails> comps) {
        final MutableDependencyGraph graph = simpleBdioFactory.createMutableDependencyGraph();
        for (final ComponentDetails comp : comps) {
            addDependency(graph, null, comp);
        }
        return graph;
    }

    private MutableDependencyGraph generateFlatGraphFromAllComponentsAllLayers(final ImageComponentHierarchy imageComponentHierarchy) {
        final MutableDependencyGraph graph = simpleBdioFactory.createMutableDependencyGraph();
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            for (final ComponentDetails comp : layer.getComponents()) {
                addDependency(graph, null, comp);
            }
        }
        return graph;
    }

    private void addDependency(final MutableDependencyGraph graph, final Dependency parent, final ComponentDetails comp) {
        final Forge componentForge = ForgeGenerator.createComponentForge(comp.getLinuxDistroName());
        logger.trace(String.format("Generating component with name: %s, version: %s, arch: %s, forge: %s", comp.getName(), comp.getVersion(), comp.getArchitecture(), componentForge.getName()));
        addCompDependencyWithGivenForge(graph, comp.getName(), comp.getVersion(), comp.getArchitecture(), componentForge, parent);
    }

    private Dependency addLayerDependency(final MutableDependencyGraph graph, final String name) {
        final Forge forge = ForgeGenerator.createLayerForge();
        final ExternalId extId = simpleBdioFactory.createPathExternalId(forge, name);
        final Dependency layerDep = simpleBdioFactory.createDependency(name, "", extId);
        logger.trace(String.format("adding layer node %s as child to dependency node tree; dataId: %s", layerDep.getName(), layerDep.getExternalId().createBdioId()));
        graph.addChildToRoot(layerDep);
        return layerDep;
    }

    private Dependency addCompDependencyWithGivenForge(final MutableDependencyGraph graph, final String name, final String version, final String arch, final Forge forge, Dependency parent) {
        final ExternalId extId = simpleBdioFactory.createArchitectureExternalId(forge, name, version, arch);
        final Dependency dep = simpleBdioFactory.createDependency(name, version, extId);
        logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dep.getName(), dep.getExternalId().createBdioId()));
        if (parent == null) {
            graph.addChildToRoot(dep);
        } else {
            graph.addChildWithParent(dep, parent);
        }
        return dep;
    }
}
