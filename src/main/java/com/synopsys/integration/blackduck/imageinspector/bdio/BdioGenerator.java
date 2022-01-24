/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
import com.synopsys.integration.bdio.model.dependency.DependencyFactory;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.image.common.LayerDetails;

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

    public SimpleBdioDocument generateBdioDocumentFromImageComponentHierarchy(String codeLocationName, String projectName,
        String projectVersion,
        String linuxDistroName, ImageComponentHierarchy imageComponentHierarchy,
        boolean organizeComponentsByLayer,
        boolean includeRemovedComponents) {

        if (organizeComponentsByLayer) {
            MutableDependencyGraph graph = generateLayeredGraphFromHierarchy(imageComponentHierarchy, includeRemovedComponents);
            return generateBdioDocumentFromGraph(codeLocationName, projectName, projectVersion, linuxDistroName, graph);
        } else {
            if (includeRemovedComponents) {
                MutableDependencyGraph graph = generateFlatGraphFromAllComponentsAllLayers(imageComponentHierarchy);
                return generateBdioDocumentFromGraph(codeLocationName, projectName, projectVersion, linuxDistroName, graph);
            } else {
                return generateFlatBdioDocumentFromComponents(codeLocationName, projectName, projectVersion, linuxDistroName, imageComponentHierarchy.getFinalComponents());
            }
        }
    }

    public SimpleBdioDocument generateFlatBdioDocumentFromComponents(String codeLocationName, String projectName,
        String projectVersion,
        String linuxDistroName, List<ComponentDetails> comps) {
        MutableDependencyGraph graph = generateFlatGraphFromComponents(comps);
        return generateBdioDocumentFromGraph(codeLocationName, projectName, projectVersion, linuxDistroName, graph);
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

    private SimpleBdioDocument generateBdioDocumentFromGraph(String codeLocationName, String projectName,
        String projectVersion,
        String linuxDistroName, MutableDependencyGraph graph) {

        Forge forge = ForgeGenerator.createProjectForge(linuxDistroName);
        ExternalIdFactory externalIdFactory = simpleBdioFactory.getExternalIdFactory();
        ExternalId projectExternalId = externalIdFactory.createNameVersionExternalId(forge, projectName, projectVersion);
        SimpleBdioDocument bdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersion, projectExternalId);
        logger.info(String.format("Returning %d components", graph.getRootDependencies().size()));
        simpleBdioFactory.populateComponents(bdioDocument, projectExternalId, graph);
        return bdioDocument;
    }

    private MutableDependencyGraph generateLayeredGraphFromHierarchy(ImageComponentHierarchy imageComponentHierarchy, boolean includeRemovedComponents) {
        MutableDependencyGraph graph = simpleBdioFactory.createMutableDependencyGraph();
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

    private MutableDependencyGraph generateFlatGraphFromComponents(List<ComponentDetails> comps) {
        MutableDependencyGraph graph = simpleBdioFactory.createMutableDependencyGraph();
        for (ComponentDetails comp : comps) {
            addDependency(graph, null, comp);
        }
        return graph;
    }

    private MutableDependencyGraph generateFlatGraphFromAllComponentsAllLayers(ImageComponentHierarchy imageComponentHierarchy) {
        MutableDependencyGraph graph = simpleBdioFactory.createMutableDependencyGraph();
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            for (ComponentDetails comp : layer.getComponents()) {
                addDependency(graph, null, comp);
            }
        }
        return graph;
    }

    private void addDependency(MutableDependencyGraph graph, Dependency parent, ComponentDetails comp) {
        Forge componentForge = ForgeGenerator.createComponentForge(comp.getLinuxDistroName());
        logger.trace(String.format("Generating component with name: %s, version: %s, arch: %s, forge: %s", comp.getName(), comp.getVersion(), comp.getArchitecture(), componentForge.getName()));
        Dependency dependency = addCompDependencyWithGivenForge(graph, comp.getName(), comp.getVersion(), comp.getArchitecture(), componentForge, parent);
        for (ComponentDetails child : comp.getDependencies()) {
            addDependency(graph, dependency, child);
        }
    }

    private Dependency addLayerDependency(MutableDependencyGraph graph, String name) {
        Forge forge = ForgeGenerator.createLayerForge();
        Dependency layerDep = dependencyFactory.createNameVersionDependency(forge, name, "");
        logger.trace(String.format("adding layer node %s as child to dependency node tree; dataId: %s", layerDep.getName(), layerDep.getExternalId().createBdioId()));
        graph.addChildToRoot(layerDep);
        return layerDep;
    }

    private Dependency addCompDependencyWithGivenForge(MutableDependencyGraph graph, String name, String version, String arch, Forge forge, Dependency parent) {
        Dependency dep = dependencyFactory.createArchitectureDependency(forge, name, version, arch);
        logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dep.getName(), dep.getExternalId().createBdioId()));
        if (parent == null) {
            graph.addChildToRoot(dep);
        } else {
            graph.addChildWithParent(dep, parent);
        }
        return dep;
    }

}
