/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class LayerEntries {
    private static final Logger logger = LoggerFactory.getLogger(LayerEntries.class);

    public static LayerEntry createLayerEntry(final FileOperations fileOperations, final TarArchiveInputStream layerInputStream, final TarArchiveEntry layerEntry, final File layerOutputDir) {
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Processing layerEntry: name: %s", fileSystemEntryName));
        // plnk and opq whiteout files are found in directories we should omit from the container file system
        if (fileSystemEntryName.equals(".wh..wh..plnk") || fileSystemEntryName.endsWith("/.wh..wh..plnk") ||
                fileSystemEntryName.equals(".wh..wh..opq") || fileSystemEntryName.endsWith("/.wh..wh..opq")) {
            return new WhiteOutOmittedDirLayerEntry(layerEntry, layerOutputDir);
        } else if (fileSystemEntryName.startsWith(".wh.") || fileSystemEntryName.contains("/.wh.")) {
            return new WhiteOutFileLayerEntry(fileOperations, layerEntry, layerOutputDir);
        } else if (layerEntry.isSymbolicLink() || layerEntry.isLink()) {
            return new LinkLayerEntry(fileOperations, layerEntry, layerOutputDir);
        } else {
            return new FileDirLayerEntry(fileOperations, layerInputStream, layerEntry, layerOutputDir);
        }
    }
}
