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
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteOutOmittedDirLayerEntry extends LayerEntry {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;

    private File otherFileToDelete;

    public WhiteOutOmittedDirLayerEntry(final TarArchiveEntry layerEntry, final File layerOutputDir) {
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
    }

    @Override
    public void processFiles() {
        logger.debug(String.format("WhiteOutOmittedDirLayerEntry: %s", layerEntry.getName()));
        final Path whiteoutFilePath = Paths.get(layerOutputDir.getAbsolutePath(), layerEntry.getName());
        otherFileToDelete = whiteoutFilePath.toFile().getParentFile();
    }

    @Override
    protected Optional<File> fileToDelete() {
        return Optional.of(otherFileToDelete);
    }

}
