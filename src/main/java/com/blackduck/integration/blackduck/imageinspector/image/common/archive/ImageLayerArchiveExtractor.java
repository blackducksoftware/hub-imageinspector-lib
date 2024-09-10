/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.blackduck.integration.blackduck.imageinspector.image.common.layerentry.LayerEntries;
import com.blackduck.integration.blackduck.imageinspector.image.common.layerentry.LayerEntry;
import com.blackduck.integration.blackduck.imageinspector.image.common.layerentry.LowerLayerFileDeleter;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;

@Component
public class ImageLayerArchiveExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ImageLayerArchiveExtractor.class);
    public static final String TARFILE_FORMAT_STRING = "tarFile: %s";
    public static final String UTF_8 = "UTF-8";

    public List<File> extractLayerTarToDir(final FileOperations fileOperations, final File tarFile, final File outputDir) throws IOException {
        logger.debug(String.format(TARFILE_FORMAT_STRING, tarFile.getAbsolutePath()));
        final TarArchiveInputStream tarFileInputStream = new TarArchiveInputStream(new FileInputStream(tarFile), UTF_8);
        return extractLayerTarToDir(fileOperations, tarFileInputStream, outputDir);
    }

    public List<File> extractLayerGzipTarToDir(final FileOperations fileOperations, final File tarFile, final File outputDir) throws IOException {
        logger.debug(String.format(TARFILE_FORMAT_STRING, tarFile.getAbsolutePath()));
        final TarArchiveInputStream tarFileInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarFile)), UTF_8);
        return extractLayerTarToDir(fileOperations, tarFileInputStream, outputDir);
    }

    public List<File> extractLayerZstdTarToDir(final FileOperations fileOperations, final File tarFile, final File outputDir) throws IOException {
        logger.debug(String.format(TARFILE_FORMAT_STRING, tarFile.getAbsolutePath()));
        final TarArchiveInputStream tarFileInputStream = new TarArchiveInputStream(new ZstdCompressorInputStream(new FileInputStream(tarFile)), UTF_8);
        return extractLayerTarToDir(fileOperations, tarFileInputStream, outputDir);
    }

    // DI calls this for a simple un-tar; there should be a lower-level just-do-the-untarring method that this and DI both use
    public List<File> extractLayerTarToDir(final FileOperations fileOperations, final TarArchiveInputStream tarFileInputStream, final File outputDir) throws IOException {
        final List<File> filesToRemove = new ArrayList<>();
        try {
            outputDir.mkdirs();
            logger.debug(String.format("outputDir: %s", outputDir.getAbsolutePath()));
            final LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();
            TarArchiveEntry tarFileEntry;
            while (null != (tarFileEntry = tarFileInputStream.getNextTarEntry())) {
                try {
                    final LayerEntry tarFileEntryHandler = LayerEntries.createLayerEntry(fileOperations, tarFileInputStream, tarFileEntry, outputDir, fileDeleter);
                    final Optional<File> otherFileToRemove = tarFileEntryHandler.process();
                    List<String> filesAdded = tarFileEntryHandler.getFilesAddedByCurrentLayer().stream()
                                                  .map(File::getAbsolutePath)
                                                  .collect(Collectors.toList());
                    fileDeleter.addFilesAddedByCurrentLayer(filesAdded);
                    if (otherFileToRemove.isPresent()) {
                        logger.debug(String.format("File/directory marked for removal: %s", otherFileToRemove.get().getAbsolutePath()));
                        filesToRemove.add(otherFileToRemove.get());
                    }
                } catch (final Exception e) {
                    logger.error(String.format("Error extracting files from layer tar: %s", e.toString()));
                }
            }
        } finally {
            IOUtils.closeQuietly(tarFileInputStream);
        }
        return filesToRemove;
    }
}
