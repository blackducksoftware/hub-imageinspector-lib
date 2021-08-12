/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ImageOrderedLayerExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<TypedArchiveFile> getOrderedLayerArchives(List<TypedArchiveFile> unOrderedLayerArchives, ManifestLayerMapping manifestLayerMapping) {
        List<TypedArchiveFile> orderedLayerArchives = new ArrayList<>(manifestLayerMapping.getLayerInternalIds().size());
        for (String layerInternalId : manifestLayerMapping.getLayerInternalIds()) {
            orderedLayerArchives.add(getLayerArchive(unOrderedLayerArchives, layerInternalId));
        }
        return orderedLayerArchives;
    }

    private TypedArchiveFile getLayerArchive(List<TypedArchiveFile> unOrderedLayerArchives, String layerInternalId) {
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
