/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.exception.IntegrationException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LayerDataExtractor {
    private final ImageLayerSorter layerSorter;

    public LayerDataExtractor(final ImageLayerSorter layerSorter) {
        this.layerSorter = layerSorter;
    }

    public List<LayerDetailsBuilder> getLayerData(List<TypedArchiveFile> unOrderedLayerArchives, FullLayerMapping fullLayerMapping) throws IntegrationException {
        AtomicInteger layerIndex = new AtomicInteger(0);
        return layerSorter.getOrderedLayerArchives(unOrderedLayerArchives, fullLayerMapping.getManifestLayerMapping()).stream()
                                                  .map(archive -> new LayerDetailsBuilder(layerIndex.get(), archive, fullLayerMapping.getLayerExternalId(layerIndex.getAndIncrement())))
                                                  .collect(Collectors.toList());
    }
}
