/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageLayerApplier {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private FileOperations fileOperations;
    private ImageLayerArchiveExtractor imageLayerArchiveExtractor; // TODO misnamed

    public ImageLayerApplier(FileOperations fileOperations, ImageLayerArchiveExtractor imageLayerArchiveExtractor) {
        this.fileOperations = fileOperations;
        this.imageLayerArchiveExtractor = imageLayerArchiveExtractor;
    }

    // image format independent: ImageLayerTar (Docker subclass does not need to override this method; but prefer composition over inheritance
    public void extractLayerTar(File destinationDir, final TypedArchiveFile layerTar) throws IOException, WrongInspectorOsException {
        logger.trace(String.format("Extracting layer: %s into %s", layerTar.getFile().getAbsolutePath(), destinationDir.getAbsolutePath()));
        final List<File> filesToRemove = imageLayerArchiveExtractor.extractLayerTarToDir(fileOperations, layerTar.getFile(), destinationDir);
        for (final File fileToRemove : filesToRemove) {
            if (fileToRemove.isDirectory()) {
                logger.trace(String.format("Removing dir marked for deletion: %s", fileToRemove.getAbsolutePath()));
                FileUtils.deleteDirectory(fileToRemove);
            } else {
                logger.trace(String.format("Removing file marked for deletion: %s", fileToRemove.getAbsolutePath()));
                fileOperations.deleteQuietly(fileToRemove);
            }
        }
    }
}
