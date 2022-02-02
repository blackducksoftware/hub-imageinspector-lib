/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common.layerentry;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class LinkLayerEntry extends LayerEntryNoFileToDelete {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileOperations fileOperations;
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;

    public LinkLayerEntry(final FileOperations fileOperations, final TarArchiveEntry layerEntry, final File layerOutputDir) {
        this.fileOperations = fileOperations;
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
    }

    @Override
    public List<File> processFiles() throws IOException {
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Processing link: %s", fileSystemEntryName));
        final Path layerOutputDirPath = layerOutputDir.toPath();
        Path startLink;
        try {
            startLink = Paths.get(layerOutputDir.getAbsolutePath(), fileSystemEntryName);
        } catch (final InvalidPathException e) {
            logger.warn(String.format("Error extracting symbolic link %s: Error creating Path object: %s", fileSystemEntryName, e.getMessage()));
            return Collections.emptyList();
        }
        if (layerEntry.isSymbolicLink()) {
            processSymbolicLink(layerOutputDirPath, startLink);
        } else if (layerEntry.isLink()) {
            processHardLink(layerOutputDirPath, startLink);
        }
        return Collections.emptyList();
    }

    private void processSymbolicLink(final Path layerOutputDirPath, final Path startLink) throws IOException {
        logger.trace("Getting link name from layer entry");
        final String linkPath = layerEntry.getLinkName();
        logger.trace(String.format("layerEntry.getLinkName()/linkPath: %s", linkPath));
        Path endLink;
        logger.trace(String.format("%s is a symbolic link: %s", layerEntry.getName(), linkPath));
        logger.trace(String.format("Calculating endLink: startLink: %s; linkPath: %s", startLink.toString(), linkPath));
        if (linkPath.startsWith("/")) {
            logger.trace(String.format("linkPath %s is absolute", linkPath));
            final String relLinkPath = linkPath.substring(1);
            endLink = layerOutputDirPath.resolve(relLinkPath);
            logger.trace(String.format("normalizing %s", endLink.toString()));
            endLink = endLink.normalize();
            logger.trace(String.format("normalized to: %s", endLink.toString()));
            final File linkFile = startLink.toFile();
            final File curDir = linkFile.getParentFile().getCanonicalFile();
            logger.trace(String.format("curDir: absolute path: %s; exists: %b", curDir.getAbsolutePath(), curDir.exists()));
            final File endFile = new File(endLink.toString());
            logger.trace(String.format("endFile: canonical path: %s; exists: %b", endFile.getCanonicalPath(), endFile.exists()));
            final Path relPath = curDir.toPath().relativize(endFile.toPath());
            logger.trace(String.format("=== relPath of %s to %s: %s", curDir.getAbsolutePath(), endFile.getAbsolutePath(), relPath));
            endLink = relPath;
        } else {
            logger.trace(String.format("linkPath %s is relative", linkPath));
            endLink = new File(linkPath).toPath();
        }
        logger.trace(String.format("endLink: %s", endLink.toString()));
        fileOperations.deleteIfExists(startLink);
        try {
            logger.trace(String.format("creating symbolic link from %s -> %s", startLink, endLink));
            fileOperations.createSymbolicLink(startLink, endLink);
        } catch (final IOException e) {
            final String msg = String.format("Error creating symbolic link from %s to %s; " + "this will not affect the results unless it affects a file needed by the package manager; " + "Error: %s", startLink.toString(),
                endLink.toString(), e.getMessage());
            logger.warn(msg);
        }
    }

    private void processHardLink(final Path layerOutputDirPath, final Path startLink) {
        logger.trace(String.format("%s is a hard link", layerEntry.getName()));
        logger.trace(String.format("Calculating endLink: startLink: %s; layerEntry.getLinkName(): %s", startLink.toString(), layerEntry.getLinkName()));
        Path endLink = layerOutputDirPath.resolve(layerEntry.getLinkName());
        logger.trace(String.format("normalizing %s", endLink.toString()));
        endLink = endLink.normalize();
        logger.trace(String.format("endLink: %s", endLink.toString()));
        logger.trace(String.format("%s is a hard link: %s -> %s", layerEntry.getName(), startLink.toString(), endLink.toString()));
        final File targetFile = endLink.toFile();
        if (!targetFile.exists()) {
            logger.warn(String.format("Attempting to create a link to %s, but it does not exist", targetFile));
        }
        fileOperations.deleteIfExists(startLink);
        try {
            fileOperations.createLink(startLink, endLink);
        } catch (final IOException e) {
            logger.warn(String.format("Error creating hard link from %s to %s; Error: %s", startLink.toString(), endLink.toString(),
                e.getMessage()));
        }
    }
}
