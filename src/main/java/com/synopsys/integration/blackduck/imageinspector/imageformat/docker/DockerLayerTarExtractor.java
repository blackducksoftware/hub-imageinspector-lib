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
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry.LayerEntries;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry.LayerEntry;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

@Component
public class DockerLayerTarExtractor {
    private static final Logger logger = LoggerFactory.getLogger(DockerLayerTarExtractor.class);

    public List<File> extractLayerTarToDir(final File tarFile, final File outputDir) throws IOException {
        final FileOperations fileOperations = new FileOperations();
        return extractLayerTarToDir(fileOperations, tarFile, outputDir);
    }

    public List<File> extractLayerTarToDir(final FileOperations fileOperations, final File tarFile, final File outputDir) throws IOException {
        logger.debug(String.format("tarFile: %s", tarFile.getAbsolutePath()));
        final List<File> filesToRemove = new ArrayList<>();
        final TarArchiveInputStream tarFileInputStream = new TarArchiveInputStream(new FileInputStream(tarFile), "UTF-8");
        try {
            outputDir.mkdirs();
            logger.debug(String.format("outputDir: %s", outputDir.getAbsolutePath()));
            TarArchiveEntry tarFileEntry;
            while (null != (tarFileEntry = tarFileInputStream.getNextTarEntry())) {
                try {
                    final LayerEntry tarFileEntryHandler = LayerEntries.createLayerEntry(fileOperations, tarFileInputStream, tarFileEntry, outputDir);
                    final Optional<File> otherFileToRemove = tarFileEntryHandler.process();
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
