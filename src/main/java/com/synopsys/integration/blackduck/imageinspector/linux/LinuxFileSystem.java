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
package com.synopsys.integration.blackduck.imageinspector.linux;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.util.Stringable;

public class LinuxFileSystem extends Stringable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final File root;
    private final FileOperations fileOperations;

    public LinuxFileSystem(final File root, final FileOperations fileOperations) {
        this.root = root;
        this.fileOperations = fileOperations;
    }

    public Optional<File> getEtcDir() {
        logger.debug(String.format("Looking in root dir %s for etc dir", root.getAbsolutePath()));
        final File etcDir = new File(root, "etc");
        if (fileOperations.isDirectory(etcDir)) {
            return Optional.of(etcDir);
        } else {
            return Optional.empty();
        }
    }

    public void writeToTarGz(final File outputTarFile) throws IOException {
        outputTarFile.getParentFile().mkdirs();
        fileOperations.logFileOwnerGroupPerms(outputTarFile.getParentFile());
        FileOutputStream fOut = null;
        BufferedOutputStream bOut = null;
        GzipCompressorOutputStream gzOut = null;
        TarArchiveOutputStream tOut = null;
        try {
            fOut = new FileOutputStream(outputTarFile);
            bOut = new BufferedOutputStream(fOut);
            gzOut = new GzipCompressorOutputStream(bOut);
            tOut = new TarArchiveOutputStream(gzOut);
            tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            addFileToTar(tOut, root, "");
        } catch (Exception unexpectedException) {
            logger.error(String.format("Unexpected error creating tar.gz file: %s", unexpectedException.getMessage()), unexpectedException);
        } finally {
            if (tOut != null) {
                tOut.finish();
                tOut.close();
            }
            if (gzOut != null) {
                gzOut.close();
            }
            if (bOut != null) {
                bOut.close();
            }
            if (fOut != null) {
                fOut.close();
            }
        }
    }

    private void addFileToTar(final TarArchiveOutputStream tOut, final File fileToAdd, final String base) {
        try {
            logger.trace(String.format("Adding to tar.gz file: %s", fileToAdd.getAbsolutePath()));
            final String entryName = base + fileToAdd.getName();
            TarArchiveEntry tarEntry = null;
            if (Files.isSymbolicLink(fileToAdd.toPath())) {
                final String linkName = Files.readSymbolicLink(fileToAdd.toPath()).toString();
                logger.trace(String.format("Creating TarArchiveEntry: %s with linkName: %s", entryName, linkName));
                tarEntry = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK);
                tarEntry.setLinkName(linkName);
                logger.trace(String.format("Created TarArchiveEntry: %s; is symlink: %b: %s", tarEntry.getName(), tarEntry.isSymbolicLink(), tarEntry.getLinkName()));
            } else {
                tarEntry = new TarArchiveEntry(fileToAdd, entryName);
            }
            logger.trace(String.format("Putting archive entry for %s into archive", fileToAdd.getAbsolutePath()));
            tOut.putArchiveEntry(tarEntry);

            if (Files.isSymbolicLink(fileToAdd.toPath())) {
                logger.trace(String.format("Closing archive entry for symlink %s", fileToAdd.getAbsolutePath()));
                tOut.closeArchiveEntry();
            } else if (fileToAdd.isFile()) {
                logger.trace(String.format("Copying data for file %s", fileToAdd.getAbsolutePath()));
                try (final InputStream fileToAddInputStream = new FileInputStream(fileToAdd)) {
                    IOUtils.copy(fileToAddInputStream, tOut);
                } catch (Exception copyException) {
                    logger.warn(String.format("Unable to copy file to archive: %s: %s", fileToAdd.getAbsolutePath(), copyException.getMessage()), copyException);
                }
                logger.trace(String.format("Closing file entry for symlink %s", fileToAdd.getAbsolutePath()));
                tOut.closeArchiveEntry();
            } else {
                logger.trace(String.format("Closing file entry for non-symlink/non-file %s", fileToAdd.getAbsolutePath()));
                tOut.closeArchiveEntry();
                final File[] children = fileToAdd.listFiles();
                if (children != null) {
                    for (final File child : children) {
                        addFileToTar(tOut, child, entryName + "/");
                    }
                }
            }
        } catch (Exception unExpectedException) {
            logger.warn(String.format("Unable to add file to archive: %s: %s", fileToAdd.getAbsolutePath(), unExpectedException.getMessage()), unExpectedException);
            try {
                tOut.closeArchiveEntry();
                logger.trace("closeArchiveEntry succeeded");
            } catch (Exception closeArchiveEntryException) {
            }
        }
    }
}
