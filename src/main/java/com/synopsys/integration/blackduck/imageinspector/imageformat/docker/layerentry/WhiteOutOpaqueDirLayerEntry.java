/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.LowerLayerFileDeleter;

public class WhiteOutOpaqueDirLayerEntry extends LayerEntryNoFileToDelete {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;
    private final LowerLayerFileDeleter fileDeleter;

    public WhiteOutOpaqueDirLayerEntry(final TarArchiveEntry layerEntry, final File layerOutputDir, final LowerLayerFileDeleter fileDeleter) {
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
        this.fileDeleter = fileDeleter;
    }

    @Override
    public List<File> processFiles() {
        logger.debug(String.format("WhiteOutOpaqueDirLayerEntry: %s", layerEntry.getName()));
        final Path whiteoutFilePath = Paths.get(layerOutputDir.getAbsolutePath(), layerEntry.getName());
        final File opaqueDir = whiteoutFilePath.getParent().toFile();
        logger.debug(String.format("Deleting/re-creating opaque dir %s", opaqueDir.getAbsolutePath()));
        fileDeleter.deleteFilesAddedByLowerLayers(opaqueDir);
        opaqueDir.mkdirs();
        return Collections.emptyList();
    }
}
