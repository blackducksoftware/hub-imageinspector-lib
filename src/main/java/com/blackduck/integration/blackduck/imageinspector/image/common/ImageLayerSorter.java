/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import com.blackduck.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.exception.IntegrationException;

import java.util.ArrayList;
import java.util.List;

public abstract class ImageLayerSorter {
    public List<TypedArchiveFile> getOrderedLayerArchives(List<TypedArchiveFile> unOrderedLayerArchives, ManifestLayerMapping manifestLayerMapping) throws IntegrationException {
        List<TypedArchiveFile> orderedLayerArchives = new ArrayList<>(manifestLayerMapping.getLayerInternalIds().size());
        for (String layerInternalId : manifestLayerMapping.getLayerInternalIds()) {
            orderedLayerArchives.add(getLayerArchive(unOrderedLayerArchives, layerInternalId));
        }
        return orderedLayerArchives;
    }

    protected abstract TypedArchiveFile getLayerArchive(List<TypedArchiveFile> unOrderedLayerArchives, String layerInternalId) throws IntegrationException;
}
