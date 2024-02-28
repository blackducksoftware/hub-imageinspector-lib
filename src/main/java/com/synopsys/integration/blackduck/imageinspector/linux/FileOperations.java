/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.synopsys.integration.exception.IntegrationException;
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
            boolean wasDeleted = false;
            try {
                wasDeleted = Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                logger.trace(String.format("Attempt to delete %s failed: %s", file.getAbsolutePath(), e.getMessage()));
            }
            if (!wasDeleted) {
                logger.debug(String.format("Unable to delete %s", file.getAbsolutePath()));
            }
        }
    }

    public void logFileOwnerGroupPerms(final File file) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        final List<String> msgs = getFileOwnerGroupPermsMsgs(file);
        for (String msg : msgs) {
            logger.debug(msg);
        }
    }

    List<String> getFileOwnerGroupPermsMsgs(final File file) {
        final List<String> msgs = new ArrayList<>(8);
        try {
            if (file == null) {
                logger.warn("File passed to getFileOwnerGroupPermsMsgs() is null");
                return msgs;
            }
            msgs.add(String.format("Current process owner: %s", System.getProperty("user.name")));
            if (!file.exists()) {
                msgs.add(String.format("File %s does not exist", file.getAbsolutePath()));
                return msgs;
            }
            if (file.isDirectory()) {
                msgs.add(String.format("File %s is a directory", file.getAbsolutePath()));
            }
            msgs.add(deriveAttributesMessage(file));
        } catch (Exception e) {
            logger.warn("getFileOwnerGroupPermsMsgs() threw an exception", e);
        }
        return msgs;
    }

    private String deriveAttributesMessage(final File file) {
        String attrsMsg;
        PosixFileAttributes attrs;
        try {
            attrs = Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class)
                        .readAttributes();
            attrsMsg = String.format("File %s: owner: %s, group: %s, perms: %s", file.getAbsolutePath(), attrs.owner().getName(), attrs.group().getName(), PosixFilePermissions.toString(attrs.permissions()));
        } catch (Exception e) {
            attrsMsg = String.format("File %s: Error getting attributes: %s", file.getAbsolutePath(), e.getMessage());
        }
        return attrsMsg;
    }

    public void deleteDirectory(File dir) {
        try {
            FileUtils.deleteDirectory(dir);
        } catch (final IOException e) {
            logger.warn(String.format("Error deleting dir %s: %s", dir.getAbsolutePath(), e.getMessage()));
        }
    }

    public void deleteDirPersistently(final File dir) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            logger.debug(String.format("Attempt #%d to delete dir %s", i, dir.getAbsolutePath()));
            deleteDirectory(dir);
            if (!dir.exists()) {
                logger.debug(String.format("Dir %s has been deleted", dir.getAbsolutePath()));
                return;
            }
            Thread.sleep(1000L);
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

    public File createTempDirectory(boolean deleteOnExit) throws IOException {
        final String prefix = String.format("ImageInspectorApi_%s_%s", Thread.currentThread().getName(), Long.toString(new Date().getTime()));
        final File temp = Files.createTempDirectory(prefix).toFile();
        if (deleteOnExit) {
            temp.deleteOnExit();
        }
        logger.debug(String.format("Created temp dir %s", temp.getAbsolutePath()));
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

    public boolean mkdir(final File newDir) {
        return newDir.mkdir();
    }

    public boolean createNewFile(final File newFile) throws IOException {
        return newFile.createNewFile();
    }

    public void pruneProblematicSymLinksRecursively(final File dir) throws IOException {
        logger.trace(String.format("pruneDanglingSymLinksRecursively: %s", dir.getAbsolutePath()));
        for (File dirEntry : dir.listFiles()) {
            if (mustPrune(dir, dirEntry)) {
                final boolean deleteSucceeded = Files.deleteIfExists(dirEntry.toPath());
                if (!deleteSucceeded) {
                    logger.warn(String.format("Delete of dangling or circular symlink %s failed", dirEntry.getAbsolutePath()));
                }
            } else if (dirEntry.isDirectory()) {
                pruneProblematicSymLinksRecursively(dirEntry);
            }
        }
    }

    private boolean mustPrune(File dir, File dirEntry) throws IOException {
        Path dirEntryAsPath = dirEntry.toPath();
        if (!Files.isSymbolicLink(dirEntryAsPath)) {
            return false;
        }
        final Path symLinkTargetPath = Files.readSymbolicLink(dirEntryAsPath);
        final File symLinkTargetFile = new File(dir, symLinkTargetPath.toString());
        Path symLinkTargetPathAdjusted = symLinkTargetFile.toPath();
        logger.trace(String.format("Found symlink %s -> %s [link value: %s]", dirEntry.getAbsolutePath(), symLinkTargetFile.getAbsolutePath(), symLinkTargetPath));
        logger.trace(String.format("Checking to see if %s starts with %s", dirEntryAsPath.normalize().toFile().getAbsolutePath(), symLinkTargetPathAdjusted.normalize().toFile().getAbsolutePath()));
        if (dirEntryAsPath.normalize().startsWith(symLinkTargetPathAdjusted.normalize())) {
            logger.debug(String.format("symlink %s lives under its target %s; this is a circular symlink that will/must be deleted", dirEntry.getAbsolutePath(), symLinkTargetFile.getAbsolutePath()));
            return true;
        }
        if (!symLinkTargetFile.exists()) {
            logger.debug(String.format("Symlink target %s does not exist; %s is a dangling symlink that will/must be deleted", symLinkTargetFile.getAbsolutePath(), dirEntry.getAbsolutePath()));
            return true;
        }
        return false;
    }
}
