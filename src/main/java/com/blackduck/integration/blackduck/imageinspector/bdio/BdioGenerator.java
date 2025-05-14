/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.bdio;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackduck.integration.bdio.BdioWriter;
import com.blackduck.integration.bdio.SimpleBdioFactory;
import com.blackduck.integration.bdio.graph.ProjectDependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.dependency.DependencyFactory;
import com.blackduck.integration.bdio.model.dependency.ProjectDependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchy;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerDetails;

@Component
public class BdioGenerator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SimpleBdioFactory simpleBdioFactory;
    private final DependencyFactory dependencyFactory;

    public BdioGenerator() {
        simpleBdioFactory = new SimpleBdioFactory();
        dependencyFactory = new DependencyFactory(new ExternalIdFactory());
    }

    public BdioGenerator(SimpleBdioFactory simpleBdioFactory, DependencyFactory dependencyFactory) {
        this.simpleBdioFactory = simpleBdioFactory;
        this.dependencyFactory = dependencyFactory;
    }

    public SimpleBdioDocument generateBdioDocumentFromImageComponentHierarchy(
        String codeLocationName, String projectName,
        String projectVersion,
        String linuxDistroName, ImageComponentHierarchy imageComponentHierarchy,
        boolean organizeComponentsByLayer,
        boolean includeRemovedComponents
    ) {
        ExternalId projectExternalId = createProjectExternalId(projectName, projectVersion, linuxDistroName);
        ProjectDependency projectDependency = createProjectDependency(projectName, projectVersion, projectExternalId);
        if (organizeComponentsByLayer) {
            ProjectDependencyGraph graph = generateLayeredGraphFromHierarchy(projectDependency, imageComponentHierarchy, includeRemovedComponents);
            return generateBdioDocumentFromGraph(codeLocationName, projectExternalId, graph);
        } else {
            if (includeRemovedComponents) {
                ProjectDependencyGraph graph = generateFlatGraphFromAllComponentsAllLayers(projectDependency, imageComponentHierarchy);
                return generateBdioDocumentFromGraph(codeLocationName, projectExternalId, graph);
            } else {
                return generateFlatBdioDocumentFromComponents(
                    projectDependency,
                    codeLocationName,
                    projectExternalId,
                    imageComponentHierarchy.getFinalComponents()
                );
            }
        }
    }

    @NotNull
    public ExternalId createProjectExternalId(String projectName, String projectVersion, String linuxDistroName) {
        Forge forge = ForgeGenerator.createProjectForge(linuxDistroName);
        ExternalIdFactory externalIdFactory = simpleBdioFactory.getExternalIdFactory();
        return externalIdFactory.createNameVersionExternalId(forge, projectName, projectVersion);
    }

    @NotNull
    public ProjectDependency createProjectDependency(String projectName, String projectVersion, ExternalId projectExternalId) {
        return new ProjectDependency(projectName, projectVersion, projectExternalId);
    }

    public SimpleBdioDocument generateFlatBdioDocumentFromComponents(
        ProjectDependency projectDependency,
        String codeLocationName, ExternalId projectExternalId, List<ComponentDetails> comps
    ) {
        ProjectDependencyGraph graph = generateFlatGraphFromComponents(projectDependency, comps);
        return generateBdioDocumentFromGraph(codeLocationName, projectExternalId, graph);
    }

    public void writeBdio(Writer writer, SimpleBdioDocument bdioDocument) throws IOException {
        try (BdioWriter bdioWriter = simpleBdioFactory.createBdioWriter(writer)) {
            simpleBdioFactory.writeSimpleBdioDocument(bdioWriter, bdioDocument);
        }
    }

    public String[] getBdioAsStringArray(SimpleBdioDocument bdioDocument) throws IOException {
        try (CharArrayWriter charArrayWriter = new CharArrayWriter()) {
            writeBdio(charArrayWriter, bdioDocument);
            String bdioString = charArrayWriter.toString();
            return bdioString.split("\n");
        }
    }

    private SimpleBdioDocument generateBdioDocumentFromGraph(
        String codeLocationName, ExternalId projectExternalId, ProjectDependencyGraph graph
    ) {
        SimpleBdioDocument bdioDocument = simpleBdioFactory.createEmptyBdioDocument(codeLocationName, projectExternalId);
        logger.info(String.format("Returning %d components", graph.getRootDependencies().size()));
        simpleBdioFactory.populateComponents(bdioDocument, graph);
        return bdioDocument;
    }

    private ProjectDependencyGraph generateLayeredGraphFromHierarchy(
        ProjectDependency projectDependency,
        ImageComponentHierarchy imageComponentHierarchy,
        boolean includeRemovedComponents
    ) {
        ProjectDependencyGraph graph = new ProjectDependencyGraph(projectDependency);
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            Dependency layerDependency = addLayerDependency(graph, layer.getLayerIndexedName());
            logger.trace(String.format("Created layer node: %s", layerDependency.getName()));
            for (ComponentDetails comp : layer.getComponents()) {
                if (imageComponentHierarchy.getFinalComponents().contains(comp)) {
                    logger.trace(String.format("layer comp %s:%s is in final components list; including it in this layer", comp.getName(), comp.getVersion()));
                    addDependency(graph, layerDependency, comp);
                } else {
                    logger.trace(String.format("layer comp %s:%s is not in final component list", comp.getName(), comp.getVersion()));
                    if (includeRemovedComponents) {
                        logger.trace("\tIncluding it in this layer");
                        addDependency(graph, layerDependency, comp);
                    } else {
                        logger.trace("\tExcluding it from this layer");
                    }
                }
            }
        }
        return graph;
    }

    private ProjectDependencyGraph generateFlatGraphFromComponents(ProjectDependency projectDependency, List<ComponentDetails> comps) {
        ProjectDependencyGraph graph = new ProjectDependencyGraph(projectDependency);
        for (ComponentDetails comp : comps) {
            addDependency(graph, null, comp);
        }
        return graph;
    }

    private ProjectDependencyGraph generateFlatGraphFromAllComponentsAllLayers(ProjectDependency projectDependency, ImageComponentHierarchy imageComponentHierarchy) {
        ProjectDependencyGraph graph = new ProjectDependencyGraph(projectDependency);
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            for (ComponentDetails comp : layer.getComponents()) {
                addDependency(graph, null, comp);
            }
        }
        return graph;
    }

    private void addDependency(ProjectDependencyGraph graph, Dependency parent, ComponentDetails comp) {
        Forge componentForge = ForgeGenerator.createComponentForge(comp.getLinuxDistroName());
        logger.trace(String.format(
            "Generating component with name: %s, version: %s, arch: %s, forge: %s", // Generating component with name: filesystem, version: 1.1-4.ph4, arch: x86_64, forge: @vmware photon os
            comp.getName(),
            comp.getVersion(),
            comp.getArchitecture(),
            componentForge.getName()
        ));
        Dependency dependency = addCompDependencyWithGivenForge(graph, comp.getName(), comp.getVersion(), comp.getArchitecture(), componentForge, parent);
        for (ComponentDetails child : comp.getDependencies()) {
            addCompDependencyWithGivenForge(graph, child.getName(), child.getVersion(), child.getArchitecture(), componentForge, dependency);
        }
    }

    private Dependency addLayerDependency(ProjectDependencyGraph graph, String name) {
        Forge forge = ForgeGenerator.createLayerForge();
        Dependency layerDep = dependencyFactory.createNameVersionDependency(forge, name, "");
        logger.trace(String.format("adding layer node %s as child to dependency node tree; dataId: %s", layerDep.getName(), layerDep.getExternalId().createBdioId()));
        graph.addDirectDependency(layerDep);
        return layerDep;
    }

    private Dependency addCompDependencyWithGivenForge(ProjectDependencyGraph graph, String name, String version, String arch, Forge forge, Dependency parent) {
        Dependency dep = dependencyFactory.createArchitectureDependency(forge, name, version, arch);
        logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dep.getName(), dep.getExternalId().createBdioId()));
        if (parent == null) {
            graph.addDirectDependency(dep);
        } else {
            graph.addChildWithParent(dep, parent);
        }
        return dep;
    }
}
