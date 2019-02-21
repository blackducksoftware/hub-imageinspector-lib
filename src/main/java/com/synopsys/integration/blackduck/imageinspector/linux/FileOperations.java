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
package com.synopsys.integration.blackduck.imageinspector.linux;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileOperations {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String readFileToString(final File inputFile) throws IOException {
        return FileUtils
            .readFileToString(inputFile, StandardCharsets.UTF_8);
    }

    public void moveFile(final File fileToMove, final File destination) throws IOException {
        final String filename = fileToMove.getName();
        logger.debug(String.format("Moving %s to %s", fileToMove.getAbsolutePath(), destination.getAbsolutePath()));
        final Path destPath = destination.toPath().resolve(filename);
        Files.move(fileToMove.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteFilesOnly(final File file) {
        if (file.isDirectory()) {
            for (final File subFile : file.listFiles()) {
                deleteFilesOnly(subFile);
            }
        } else {
            file.delete();
        }
    }

    public void logFileOwnerGroupPerms(final File file) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug(String.format("Current process owner: %s", System.getProperty("user.name")));
        if (!file.exists()) {
            logger.debug(String.format("File %s does not exist", file.getAbsolutePath()));
            return;
        }
        if (file.isDirectory()) {
            logger.debug(String.format("File %s is a directory", file.getAbsolutePath()));
        }
        PosixFileAttributes attrs;
        try {
            attrs = Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class)
                        .readAttributes();
            logger.debug(String.format("File %s: owner: %s, group: %s, perms: %s", file.getAbsolutePath(), attrs.owner().getName(), attrs.group().getName(), PosixFilePermissions.toString(attrs.permissions())));
        } catch (final IOException e) {
            logger.debug(String.format("File %s: Error getting attributes: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

    public void deleteDirPersistently(final File dir) {
        for (int i = 0; i < 10; i++) {
            logger.debug(String.format("Attempt #%d to delete dir %s", i, dir.getAbsolutePath()));
            try {
                FileUtils.deleteDirectory(dir);
            } catch (final IOException e) {
                logger.warn(String.format("Error deleting dir %s: %s", dir.getAbsolutePath(), e.getMessage()));
            }
            if (!dir.exists()) {
                logger.debug(String.format("Dir %s has been deleted", dir.getAbsolutePath()));
                return;
            }
            try {
                Thread.sleep(1000L);
            } catch (final InterruptedException e) {
                logger.warn(String.format("deleteDir() sleep interrupted: %s", e.getMessage()));
            }
        }
        logger.warn(String.format("Unable to delete dir %s", dir.getAbsolutePath()));
    }

    public void logFreeDiskSpace(final File dir) {
        logger.debug(String.format("Disk: free: %d", dir.getFreeSpace()));
    }

    public void deleteFile(File fileToDelete) throws IOException {
        Files.delete(fileToDelete.toPath());
    }

    public void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        IOUtils.copy(inputStream, outputStream);
    }

    public void deleteIfExists(final Path pathToDelete) {
        try {
            Files.delete(pathToDelete); // remove lower layer's version if exists
        } catch (final IOException e) {
            // expected (most of the time)
        }
    }

    public void createSymbolicLink(final Path startLink, final Path endLink) throws IOException {
        Files.createSymbolicLink(startLink, endLink);
    }

    public void createLink(final Path startLink, final Path endLink) throws IOException {
        Files.createLink(startLink, endLink);
    }

    public File createTempDirectory() throws IOException {
        final String suffix = String.format("_%s_%s", Thread.currentThread().getName(), Long.toString(new Date().getTime()));
        final File temp = File.createTempFile("ImageInspectorApi_", suffix);
        logger.info(String.format("Creating working dir %s", temp.getAbsolutePath()));
        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        logFreeDiskSpace(temp);
        return temp;
    }

    public void deleteQuietly(final File file) {
        FileUtils.deleteQuietly(file);
    }

    public File[] listFilesInDir(final File dir) {
        return dir.listFiles();
    }

    public boolean isDirectory(final File dir) {
        return dir.isDirectory();
    }
}
