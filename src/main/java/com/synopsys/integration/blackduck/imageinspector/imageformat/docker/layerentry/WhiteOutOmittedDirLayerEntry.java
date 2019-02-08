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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteOutOmittedDirLayerEntry implements LayerEntry {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;

    public WhiteOutOmittedDirLayerEntry(final TarArchiveEntry layerEntry, final File layerOutputDir) {
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
    }

    @Override
    public Optional<File> process() {
        logger.debug(String.format("WhiteOutOmittedDirLayerEntry: %s", layerEntry.getName()));
        final Path whiteoutFilePath = Paths.get(layerOutputDir.getAbsolutePath(), layerEntry.getName());
        final File otherFileToDelete = whiteoutFilePath.toFile().getParentFile();
        return Optional.of(otherFileToDelete);
    }

}
