/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ImageLayerArchives {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<TypedArchiveFile> unOrderedLayerArchives;
    private final ManifestLayerMapping manifestLayerMapping;

    public ImageLayerArchives(List<TypedArchiveFile> unOrderedLayerArchives, ManifestLayerMapping manifestLayerMapping) {
        this.unOrderedLayerArchives = unOrderedLayerArchives;
        this.manifestLayerMapping = manifestLayerMapping;
    }

    public List<TypedArchiveFile> getOrderedLayerArchives() {
        List<TypedArchiveFile> orderedLayerArchives = new ArrayList<>(manifestLayerMapping.getLayerInternalIds().size());
        for (String layerInternalId : manifestLayerMapping.getLayerInternalIds()) {
            orderedLayerArchives.add(getLayerArchive(layerInternalId));
        }
        return orderedLayerArchives;
    }

    private TypedArchiveFile getLayerArchive(String layerInternalId) {
        TypedArchiveFile layerArchive = null;
        for (final TypedArchiveFile candidateLayerTar : unOrderedLayerArchives) {
            if (layerInternalId.equals(candidateLayerTar.getFile().getParentFile().getName())) {
                logger.trace(String.format("Found layer archive for layer %s: ", layerInternalId, candidateLayerTar.getFile().getAbsolutePath()));
                layerArchive = candidateLayerTar;
                break;
            }
        }
        return layerArchive;
    }
}
