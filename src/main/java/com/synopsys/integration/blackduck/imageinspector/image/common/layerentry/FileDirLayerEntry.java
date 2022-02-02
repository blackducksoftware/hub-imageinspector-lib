/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common.layerentry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class FileDirLayerEntry extends LayerEntryNoFileToDelete {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileOperations fileOperations;
    private final TarArchiveInputStream layerInputStream;
    private final TarArchiveEntry archiveEntry;
    private final File layerOutputDir;

    public FileDirLayerEntry(final FileOperations fileOperations, final TarArchiveInputStream layerInputStream, final TarArchiveEntry archiveEntry, final File layerOutputDir) {
        this.fileOperations = fileOperations;
        this.layerInputStream = layerInputStream;
        this.archiveEntry = archiveEntry;
        this.layerOutputDir = layerOutputDir;
    }

    @Override
    public List<File> processFiles() {
        final String fileSystemEntryName = archiveEntry.getName();
        logger.trace(String.format("Processing file/dir: %s", fileSystemEntryName));

        final File outputFile = new File(layerOutputDir, fileSystemEntryName);
        if (archiveEntry.isFile()) {
            logger.trace(String.format("Processing file: %s", fileSystemEntryName));
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            logger.trace(String.format("Creating output stream for %s", outputFile.getAbsolutePath()));
            OutputStream outputFileStream = null;
            try {
                outputFileStream = new FileOutputStream(outputFile);
            } catch (final FileNotFoundException e1) {
                logger.warn(String.format("Error creating output stream for %s: %s", outputFile.getAbsolutePath(), e1.getMessage()));
                logger.trace(String.format("Stacktrace for error creating output stream for %s", outputFile.getAbsolutePath()), e1);
                return Collections.emptyList();
            }
            try {
                fileOperations.copy(layerInputStream, outputFileStream);
            } catch (final IOException e) {
                logger.error(String.format("Error copying file %s to %s: %s", fileSystemEntryName, outputFile.getAbsolutePath(), e.getMessage()));
            } finally {
                try {
                    outputFileStream.close();
                } catch (final IOException e) {
                    logger.error(String.format("Error closing output file stream for: %s: %s", outputFile.getAbsolutePath(), e.getMessage()));
                }
            }
        } else {
            final boolean mkdirSucceeded = outputFile.mkdirs();
            if (!mkdirSucceeded) {
                logger.trace(String.format("mkdir of %s didn't succeed, but it might have already existed", outputFile.getAbsolutePath()));
            }
        }
        return Collections.singletonList(outputFile);
    }
}
