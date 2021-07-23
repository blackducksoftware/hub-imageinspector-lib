/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common.layerentry;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.LowerLayerFileDeleter;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class WhiteOutFileLayerEntry extends LayerEntryNoFileToDelete {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileOperations fileOperations;
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;
    private final LowerLayerFileDeleter fileDeleter;

    public WhiteOutFileLayerEntry(final FileOperations fileOperations, final TarArchiveEntry layerEntry, final File layerOutputDir,
        final LowerLayerFileDeleter fileDeleter) {
        this.fileOperations = fileOperations;
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
        this.fileDeleter = fileDeleter;
    }

    @Override
    public List<File> processFiles() {
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Found white-out file %s", fileSystemEntryName));

        final int whiteOutMarkIndex = fileSystemEntryName.indexOf(".wh.");
        if (whiteOutMarkIndex < 0) {
            logger.warn(String.format("%s is not a valid WhiteOutFileLayerEntry; does not contain '.wh.'", fileSystemEntryName));
            return Collections.emptyList();
        }
        final String beforeWhiteOutMark = fileSystemEntryName.substring(0, whiteOutMarkIndex);
        final String afterWhiteOutMark = fileSystemEntryName.substring(whiteOutMarkIndex + ".wh.".length());

        final String filePathToRemove = String.format("%s%s", beforeWhiteOutMark, afterWhiteOutMark);
        final File fileToRemove = new File(layerOutputDir, filePathToRemove);
        logger.trace(String.format("Removing %s from image (this layer whites it out)", filePathToRemove));
        if (fileToRemove.isDirectory()) {
            try {
                fileDeleter.deleteFilesAddedByLowerLayers(fileToRemove);
                logger.trace(String.format("Directory %s successfully removed", filePathToRemove));
            } catch (final Exception e) {
                logger.warn(String.format("Error removing whited-out directory %s", filePathToRemove));
            }
        } else {
            try {
                fileDeleter.deleteFilesAddedByLowerLayers(fileToRemove); // TODO - should FileOperations handle this deletion?
                logger.trace(String.format("File %s successfully removed", filePathToRemove));
            } catch (final Exception e) {
                logger.warn(String.format("Error removing whited-out file %s", filePathToRemove));
            }
        }
        return Collections.emptyList();
    }
}
