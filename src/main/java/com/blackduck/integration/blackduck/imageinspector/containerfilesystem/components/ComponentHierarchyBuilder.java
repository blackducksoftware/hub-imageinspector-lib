/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.PackageGetter;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerDetailsBuilder;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerDetails;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;
import org.apache.commons.collections.ListUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ComponentHierarchyBuilder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PackageGetter packageGetter;
    private final ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy();
    @Nullable
    private Integer platformTopLayerIndex;

    public ComponentHierarchyBuilder(PackageGetter packageGetter) {
        this.packageGetter = packageGetter;
    }

    public ComponentHierarchyBuilder setPlatformTopLayerIndex(int platformTopLayerIndex) {
        this.platformTopLayerIndex = platformTopLayerIndex;
        return this;
    }

    public ComponentHierarchyBuilder addLayer(ContainerFileSystemWithPkgMgrDb postLayerContainerFileSystem, LayerDetailsBuilder layerData) {
        int layerIndex = layerData.getLayerIndex();
        logger.info("Querying pkg mgr for components after adding layer {}", layerIndex);
        final List<ComponentDetails> comps = packageGetter.queryPkgMgrForDependencies(postLayerContainerFileSystem);
        logger.info(String.format("Found %d components in file system after adding layer %d", comps.size(), layerIndex));
        for (ComponentDetails comp : comps) {
            logger.trace(String.format("\t%s/%s/%s", comp.getName(), comp.getVersion(), comp.getArchitecture()));
        }
        final LayerDetails layer = layerData.build(comps);
        imageComponentHierarchy.addLayer(layer);
        if ((platformTopLayerIndex != null) && (layerIndex == platformTopLayerIndex)) {
            imageComponentHierarchy.setPlatformComponents(comps);
        }
        return this;
    }

    public ImageComponentHierarchy build() {
        int numLayers = imageComponentHierarchy.getLayers().size();
        if (numLayers > 0) {
            LayerDetails topLayer = imageComponentHierarchy.getLayers().get(numLayers - 1);
            final List<ComponentDetails> netComponents = getNetComponents(topLayer.getComponents(), imageComponentHierarchy.getPlatformComponents());
            imageComponentHierarchy.setFinalComponents(netComponents);
        }
        if (platformTopLayerIndex != null) {
            imageComponentHierarchy.setPlatformTopLayerIndex(platformTopLayerIndex);
        }
        return imageComponentHierarchy;
    }

    private List<ComponentDetails> getNetComponents(final List<ComponentDetails> grossComponents, final List<ComponentDetails> componentsToOmit) {
        logger.info(String.format("There are %d components to omit", componentsToOmit.size()));
        if (componentsToOmit.isEmpty()) {
            return grossComponents;
        }
        List<ComponentDetails> netComponents = ListUtils.subtract(grossComponents, componentsToOmit);
        logger.debug(String.format("grossComponents: %d, componentsToOmit: %d, netComponents: %d", grossComponents.size(), componentsToOmit.size(), netComponents.size()));
        return netComponents;
    }

    public Optional<Integer> getPlatformTopLayerIndex() {
        return Optional.ofNullable(platformTopLayerIndex);
    }
}
