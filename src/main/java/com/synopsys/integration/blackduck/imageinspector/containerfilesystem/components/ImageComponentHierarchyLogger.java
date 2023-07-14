/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components;

import com.synopsys.integration.blackduck.imageinspector.image.common.LayerDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ImageComponentHierarchyLogger {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void log(final ImageComponentHierarchy imageComponentHierarchy) {
        if (!logger.isTraceEnabled()) {
            return;
        }
        logger.trace("layer dump:");
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            if (layer == null) {
                logger.trace("Layer is null");
            } else if (layer.getComponents() == null) {
                logger.trace(String.format("layer %s has no componenents", layer.getLayerIndexedName()));
            } else {
                logger.trace(String.format("Layer %s has %d components; layer cmd: %s", layer.getLayerIndexedName(), layer.getComponents().size(), layer.getLayerCmd()));
            }
        }
        if (imageComponentHierarchy.getFinalComponents() == null) {
            logger.trace("Final image components list not set");
        } else {
            logger.trace(String.format("Final image components list has %d components", imageComponentHierarchy.getFinalComponents().size()));
        }
    }
}
