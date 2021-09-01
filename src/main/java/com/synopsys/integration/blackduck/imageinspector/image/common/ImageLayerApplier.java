/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ImageLayerArchiveExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Component
public class ImageLayerApplier {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileOperations fileOperations;
    private final ImageLayerArchiveExtractor imageLayerArchiveExtractor;

    public ImageLayerApplier(FileOperations fileOperations, ImageLayerArchiveExtractor imageLayerArchiveExtractor) {
        this.fileOperations = fileOperations;
        this.imageLayerArchiveExtractor = imageLayerArchiveExtractor;
    }

    public void applyLayer(File destinationDir, final TypedArchiveFile layerTar) throws IOException, WrongInspectorOsException {
        logger.trace(String.format("Extracting layer: %s into %s", layerTar.getFile().getAbsolutePath(), destinationDir.getAbsolutePath()));
        List<File> filesToRemove = new LinkedList<>();
        if (layerTar.getType().equals(ArchiveFileType.TAR)) {
            filesToRemove = imageLayerArchiveExtractor.extractLayerTarToDir(fileOperations, layerTar.getFile(), destinationDir);
        } else if (layerTar.getType().equals(ArchiveFileType.TAR_GZIPPED)) {
            filesToRemove = imageLayerArchiveExtractor.extractLayerGzipTarToDir(fileOperations, layerTar.getFile(), destinationDir);
        } else if (layerTar.getType().equals(ArchiveFileType.TAR_ZSTD)) {
            //TODO- test this works
            filesToRemove = imageLayerArchiveExtractor.extractLayerZstdTarToDir(fileOperations, layerTar.getFile(), destinationDir);
        }
        for (final File fileToRemove : filesToRemove) {
            if (fileToRemove.isDirectory()) {
                logger.trace(String.format("Removing dir marked for deletion: %s", fileToRemove.getAbsolutePath()));
                fileOperations.deleteDirectory(fileToRemove);
            } else {
                logger.trace(String.format("Removing file marked for deletion: %s", fileToRemove.getAbsolutePath()));
                fileOperations.deleteQuietly(fileToRemove);
            }
        }
    }
}
