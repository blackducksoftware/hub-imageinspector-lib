/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DockerUnorderedLayerTars {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<TypedArchiveFile> unOrderedLayerTars;
    private final ManifestLayerMapping manifestLayerMapping;

    public DockerUnorderedLayerTars(List<TypedArchiveFile> unOrderedLayerTars, ManifestLayerMapping manifestLayerMapping) {
        this.unOrderedLayerTars = unOrderedLayerTars;
        this.manifestLayerMapping = manifestLayerMapping;
    }

    public List<TypedArchiveFile> getOrderedLayerTars() {
        List<TypedArchiveFile> orderedLayerTars = new ArrayList<>(manifestLayerMapping.getLayerInternalIds().size());
        for (String layerInternalId : manifestLayerMapping.getLayerInternalIds()) {
            orderedLayerTars.add(getLayerTar(layerInternalId));
        }
        return orderedLayerTars;
    }

    private TypedArchiveFile getLayerTar(String layerInternalId) {
        TypedArchiveFile layerTar = null;
        for (final TypedArchiveFile candidateLayerTar : unOrderedLayerTars) {
            if (layerInternalId.equals(candidateLayerTar.getFile().getParentFile().getName())) {
                logger.trace(String.format("Found layer tar for layer %s", layerInternalId));
                layerTar = candidateLayerTar;
                break;
            }
        }
        return layerTar;
    }
}
