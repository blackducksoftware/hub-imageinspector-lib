/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry;

import java.io.File;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class LayerEntries {
    private static final Logger logger = LoggerFactory.getLogger(LayerEntries.class);

    private LayerEntries() {
    }

    public static LayerEntry createLayerEntry(final FileOperations fileOperations, final TarArchiveInputStream layerInputStream, final TarArchiveEntry layerEntry, final File layerOutputDir) {
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Processing layerEntry: name: %s", fileSystemEntryName));
        // plnk whiteout files are found in directories we should omit from the container file system
        // opq (opaque) whiteout files mean don't use any siblings from lower layers; start this dir from scratch
        // NOTE: Current implementation assumes the .wh..wh..opq files is the first entry in the dir
        // (for the current layer), because (a) that's the only way I've ever seen it, and
        // (b) it seems crazy (very hard to handle) to do it any other way.
        if (fileSystemEntryName.equals(".wh..wh..plnk") || fileSystemEntryName.endsWith("/.wh..wh..plnk")) {
            return new WhiteOutOmittedDirLayerEntry(layerEntry, layerOutputDir);
        } else if (fileSystemEntryName.equals(".wh..wh..opq") || fileSystemEntryName.endsWith("/.wh..wh..opq")) {
            return new WhiteOutOpaqueDirLayerEntry(layerEntry, layerOutputDir);
        } else if (fileSystemEntryName.startsWith(".wh.") || fileSystemEntryName.contains("/.wh.")) {
            return new WhiteOutFileLayerEntry(fileOperations, layerEntry, layerOutputDir);
        } else if (layerEntry.isSymbolicLink() || layerEntry.isLink()) {
            return new LinkLayerEntry(fileOperations, layerEntry, layerOutputDir);
        } else {
            return new FileDirLayerEntry(fileOperations, layerInputStream, layerEntry, layerOutputDir);
        }
    }
}
