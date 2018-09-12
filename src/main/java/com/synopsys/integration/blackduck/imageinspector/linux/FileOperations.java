/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileOperations {
    private static final String[] DIRS_TO_SKIP = { "/Users", "/proc", "/dev", "/sys", "/tmp" };
    private static final Logger logger = LoggerFactory.getLogger(FileOperations.class);

    public static List<File> findDirWithName(final int maxDepth, final File dirFile, final String targetName) {
        final List<File> results = new ArrayList<>();
        logger.trace(String.format("Looking in %s for Dir %s", dirFile.getAbsolutePath(), targetName));
        final List<File> iter = findDirsWithGivenName(maxDepth, 0, dirFile, targetName);
        logger.trace("Processing the list...");
        for (final File f : iter) {
            logger.trace(String.format("Match: %s", f.getAbsolutePath()));
            results.add(f);
        }
        logger.trace("Done processing the list");
        return results;
    }

    public static List<File> findFilesWithExt(final File dirFile, final String fileExtension) {
        final List<File> results = new ArrayList<>();
        logger.trace(String.format("Looking in %s for files with extension %s", dirFile.getAbsolutePath(), fileExtension));
        final IOFileFilter fileFilter = new WildcardFileFilter("*." + fileExtension);
        final IOFileFilter dirFilter = TrueFileFilter.INSTANCE;
        final Iterator<File> iter = FileUtils.iterateFilesAndDirs(dirFile, fileFilter, dirFilter);
        while (iter.hasNext()) {
            final File f = iter.next();
            if (f.isFile()) {
                logger.trace(String.format("Match: %s", f.getAbsolutePath()));
                results.add(f);
            }
        }
        return results;
    }

    public static void copyFile(final File fileToCopy, final File destination) throws IOException {
        ensureDirExists(destination);
        final String filename = fileToCopy.getName();
        logger.debug(String.format("Copying %s to %s", fileToCopy.getAbsolutePath(), destination.getAbsolutePath()));
        final Path destPath = destination.toPath().resolve(filename);
        Files.copy(fileToCopy.toPath(), destPath);
    }

    public static void moveFile(final File fileToMove, final File destination) throws IOException {
        final String filename = fileToMove.getName();
        logger.debug(String.format("Moving %s to %s", fileToMove.getAbsolutePath(), destination.getAbsolutePath()));
        final Path destPath = destination.toPath().resolve(filename);
        Files.move(fileToMove.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyDirContentsToDir(final String fromDirPath, final String toDirPath, final boolean createIfNecessary) throws IOException {
        final File srcDir = new File(fromDirPath);
        final File destDir = new File(toDirPath);
        if (createIfNecessary && !destDir.exists()) {
            destDir.mkdirs();
        }
        FileUtils.copyDirectory(srcDir, destDir);
    }

    public static void removeFileOrDirQuietly(final String fileOrDirPath) {
        try {
            removeFileOrDir(fileOrDirPath);
        } catch (final IOException e) {
            logger.warn(String.format("Error removing file or directory %s: ", fileOrDirPath, e.getMessage()));
        }
    }

    public static void removeFileOrDir(final String fileOrDirPath) throws IOException {
        logger.info(String.format("Removing file or dir: %s", fileOrDirPath));
        final File fileOrDir = new File(fileOrDirPath);
        if (fileOrDir.exists()) {
            if (fileOrDir.isDirectory()) {
                FileUtils.deleteDirectory(fileOrDir);
            } else {
                FileUtils.deleteQuietly(fileOrDir);
            }
        }
    }

    public static void deleteFilesOnly(final File file) {
        if (file.isDirectory()) {
            for (final File subFile : file.listFiles()) {
                deleteFilesOnly(subFile);
            }
        } else {
            file.delete();
        }
    }

    public static void ensureDirExists(final File dir) {
        logger.debug(String.format("Creating %s (if it does not exist)", dir.getAbsolutePath()));
        final boolean mkdirsResult = dir.mkdirs();
        logger.debug(String.format("\tmkdirs result: %b", mkdirsResult));
    }

    public static void logFileOwnerGroupPerms(final File file) {
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

    public static File purgeDir(final String dirPath) {
        logger.trace(String.format("Purging/recreating dir: %s", dirPath));
        final File dir = new File(dirPath);
        try {
            FileUtils.deleteDirectory(dir);
            dir.mkdirs();
        } catch (final IOException e) {
            logger.warn(String.format("Error purging dir: %s", dir.getAbsolutePath()));
        }
        logger.trace(String.format("dirPath %s: exists: %b; isDirectory: %b", dirPath, dir.exists(), dir.isDirectory()));
        if (dir.listFiles() != null) {
            logger.trace(String.format("dirPath %s: # files: %d", dirPath, dir.listFiles().length));
        }
        return dir;
    }

    public static void deleteDirPersistently(final File dir) {
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

    public static void logFreeDiskSpace(final File dir) {
        logger.debug(String.format("Disk: free: %d", dir.getFreeSpace()));
    }

    private static List<File> findDirsWithGivenName(final int maxDepth, final int depth, final File dir, final String targetName) {
        final List<File> filesMatchingTargetName = new ArrayList<>();
        logger.trace(String.format("findFiles() processing dir %s", dir.getAbsolutePath()));
        String dirCanonicalPath;
        try {
            dirCanonicalPath = dir.getCanonicalPath();
        } catch (final IOException e1) {
            dirCanonicalPath = dir.getAbsolutePath();
        }
        if (depth > maxDepth) {
            logger.trace("Hit max depth; pruning tree here");
            return filesMatchingTargetName;
        }
        // TODO IDOCKER-508: this compares relative path like /Users to full path!
        for (final String dirToSkip : DIRS_TO_SKIP) {
            if (dirToSkip.equals(dirCanonicalPath)) {
                logger.trace(String.format("dir %s is in the skip list; skipping it", dir.getAbsolutePath()));
                return filesMatchingTargetName;
            }
        }
        try {
            for (final File fileWithinDir : dir.listFiles()) {
                if (fileWithinDir.isDirectory()) {
                    if (targetName.equals(fileWithinDir.getName())) {
                        filesMatchingTargetName.add(fileWithinDir);
                    }
                    final List<File> subDirsMatchingFiles = findDirsWithGivenName(maxDepth, depth + 1, fileWithinDir, targetName);
                    filesMatchingTargetName.addAll(subDirsMatchingFiles);
                }
            }
        } catch (final Throwable e) {
            logger.debug("Error reading contents of dir; skipping it");
        }
        return filesMatchingTargetName;
    }
}
