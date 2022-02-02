/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.docker;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerSorter;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;

public class DockerImageLayerSorter extends ImageLayerSorter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected TypedArchiveFile getLayerArchive(final List<TypedArchiveFile> unOrderedLayerArchives, final String layerInternalId) {
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
