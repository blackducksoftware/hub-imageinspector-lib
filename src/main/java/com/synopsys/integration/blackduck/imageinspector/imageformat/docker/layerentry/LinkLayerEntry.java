/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class LinkLayerEntry implements LayerEntry {
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
    public Optional<File> process() throws IOException {
        final Optional<File> otherFileToDeleteNone = Optional.empty();
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Processing link: %s", fileSystemEntryName));
        final Path layerOutputDirPath = layerOutputDir.toPath();
        Path startLink;
        try {
            startLink = Paths.get(layerOutputDir.getAbsolutePath(), fileSystemEntryName);
        } catch (final InvalidPathException e) {
            logger.warn(String.format("Error extracting symbolic link %s: Error creating Path object: %s", fileSystemEntryName, e.getMessage()));
            return otherFileToDeleteNone;
        }
        if (layerEntry.isSymbolicLink()) {
            processSymbolicLink(layerOutputDirPath, startLink);
        } else if (layerEntry.isLink()) {
            processHardLink(layerOutputDirPath, startLink);
        }
        return otherFileToDeleteNone;
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

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
