/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.PackageGetter;
import com.synopsys.integration.blackduck.imageinspector.image.common.LayerDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;
import org.apache.commons.collections.ListUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    public ComponentHierarchyBuilder addLayer(ContainerFileSystemWithPkgMgrDb postLayerContainerFileSystem, int layerIndex, String layerExternalId, List<String> layerCmd) {
        logger.info("Querying pkg mgr for components after adding layer {}", layerIndex);
        final List<ComponentDetails> comps = packageGetter.queryPkgMgrForDependencies(postLayerContainerFileSystem);
        logger.info(String.format("Found %d components in file system after adding layer %d", comps.size(), layerIndex));
        for (ComponentDetails comp : comps) {
            logger.trace(String.format("\t%s/%s/%s", comp.getName(), comp.getVersion(), comp.getArchitecture()));
        }
        final LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerCmd, comps);
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
}
