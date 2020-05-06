package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteOutOpaqueDirLayerEntry extends LayerEntryNoFileToDelete {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;

    public WhiteOutOpaqueDirLayerEntry(final TarArchiveEntry layerEntry, final File layerOutputDir) {
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
    }

    @Override
    public void processFiles() {
        logger.debug(String.format("WhiteOutOpaqueDirLayerEntry: %s", layerEntry.getName()));
        final Path whiteoutFilePath = Paths.get(layerOutputDir.getAbsolutePath(), layerEntry.getName());
        final File opaqueDir = whiteoutFilePath.getParent().toFile();
        logger.debug(String.format("Deleting/re-creating opaque dir %s", opaqueDir.getAbsolutePath()));
        FileUtils.deleteQuietly(opaqueDir);
        opaqueDir.mkdirs();
    }
}
