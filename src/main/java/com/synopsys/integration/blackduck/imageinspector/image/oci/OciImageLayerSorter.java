/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.util.List;

import com.synopsys.integration.exception.IntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerSorter;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;

public class OciImageLayerSorter extends ImageLayerSorter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected TypedArchiveFile getLayerArchive(final List<TypedArchiveFile> unOrderedLayerArchives, final String layerInternalId) throws IntegrationException {
        TypedArchiveFile layerArchive = null;
        for (final TypedArchiveFile candidateLayerTar : unOrderedLayerArchives) {
            String candidateId = String.format("%s:%s", candidateLayerTar.getFile().getParentFile().getName(), candidateLayerTar.getFile().getName());
            if (layerInternalId.equals(candidateId)) {
                logger.trace(String.format("Found layer archive for layer %s: ", layerInternalId, candidateLayerTar.getFile().getAbsolutePath()));
                layerArchive = candidateLayerTar;
                break;
            }
        }
        if (layerArchive == null) {
            throw new IntegrationException(String.format("Unable to locate layer archive file for internal layer ID: %s", layerInternalId));
        }
        return layerArchive;
    }
}
