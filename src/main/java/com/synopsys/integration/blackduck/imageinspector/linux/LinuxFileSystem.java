/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.linux;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.Stringable;

public class LinuxFileSystem extends Stringable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final File root;
    private final FileOperations fileOperations;

    public LinuxFileSystem(File root, FileOperations fileOperations) {
        this.root = root;
        this.fileOperations = fileOperations;
    }

    public Optional<File> getEtcDir() {
        logger.debug(String.format("Looking in root dir %s for etc dir", root.getAbsolutePath()));
        File etcDir = new File(root, "etc");
        if (fileOperations.isDirectory(etcDir)) {
            return Optional.of(etcDir);
        } else {
            return Optional.empty();
        }
    }

    public void writeToTarGz(File outputTarFile, String containerFileSystemExcludedPathListString) {
        outputTarFile.getParentFile().mkdirs();
        fileOperations.logFileOwnerGroupPerms(outputTarFile.getParentFile());
        try (FileOutputStream fOut = new FileOutputStream(outputTarFile);
            BufferedOutputStream bOut = new BufferedOutputStream(fOut);
            GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(bOut)) {
            try (TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut)) {
                tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                tOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                List<String> containerFileSystemExcludedPathList = cleanPaths(getListFromCommaSeparatedString(containerFileSystemExcludedPathListString));
                String rootName = root.getName();
                addFileToTar(tOut, rootName, root, null, containerFileSystemExcludedPathList);
            }
        } catch (Exception unexpectedException) {
            logger.error(String.format("Unexpected error creating tar.gz file: %s", unexpectedException.getMessage()), unexpectedException);
        }
    }

    private void addFileToTar(TarArchiveOutputStream tOut, String rootName, File fileToAdd, String base, List<String> containerFileSystemExcludedPathList) {
        try {
            logger.trace(String.format("Adding to tar.gz file: %s", fileToAdd.getAbsolutePath()));
            base = base == null ? "" : base;
            String entryName = base + fileToAdd.getName();
            if (isExcluded(rootName, entryName, containerFileSystemExcludedPathList)) {
                return;
            }
            TarArchiveEntry tarEntry = createTarArchiveEntry(fileToAdd, entryName);
            logger.trace(String.format("Putting archive entry for %s into archive", fileToAdd.getAbsolutePath()));
            tOut.putArchiveEntry(tarEntry);

            if (Files.isSymbolicLink(fileToAdd.toPath())) {
                logger.trace(String.format("Closing archive entry for symlink %s", fileToAdd.getAbsolutePath()));
                tOut.closeArchiveEntry();
            } else if (fileToAdd.isFile()) {
                copyFileData(tOut, fileToAdd);
                logger.trace(String.format("Closing file entry for symlink %s", fileToAdd.getAbsolutePath()));
                tOut.closeArchiveEntry();
            } else {
                logger.trace(String.format("Closing file entry for non-symlink/non-file %s", fileToAdd.getAbsolutePath()));
                tOut.closeArchiveEntry();
                File[] children = fileToAdd.listFiles();
                if (children != null) {
                    for (File child : children) {
                        addFileToTar(tOut, rootName, child, entryName + "/", containerFileSystemExcludedPathList);
                    }
                }
            }
        } catch (Exception unExpectedException) {
            logger.warn(String.format("Unable to add file to archive: %s: %s", fileToAdd.getAbsolutePath(), unExpectedException.getMessage()), unExpectedException);
            try {
                tOut.closeArchiveEntry();
                logger.trace("closeArchiveEntry succeeded");
            } catch (Exception closeArchiveEntryException) {
                logger.trace("closeArchiveEntry failed");
            }
        }
    }

    private boolean isExcluded(String rootName, String entryName, List<String> containerFileSystemExcludedPathList) throws IntegrationException {
        String entryNameMadeAbsolute = toAbsolute(rootName, entryName);
        if (containerFileSystemExcludedPathList.contains(entryNameMadeAbsolute)) {
            logger.info(String.format("Pruning %s from tar file because it maps to excluded path %s", entryName, entryNameMadeAbsolute));
            return true;
        }
        return false;
    }

    private void copyFileData(TarArchiveOutputStream tOut, File fileToAdd) {
        logger.trace(String.format("Copying data for file %s", fileToAdd.getAbsolutePath()));
        try (InputStream fileToAddInputStream = new FileInputStream(fileToAdd)) {
            IOUtils.copy(fileToAddInputStream, tOut);
        } catch (Exception copyException) {
            logger.warn(String.format("Unable to copy file to archive: %s: %s", fileToAdd.getAbsolutePath(), copyException.getMessage()), copyException);
        }
    }

    private TarArchiveEntry createTarArchiveEntry(File fileToAdd, String entryName) throws IOException {
        TarArchiveEntry tarEntry;
        if (Files.isSymbolicLink(fileToAdd.toPath())) {
            String linkName = Files.readSymbolicLink(fileToAdd.toPath()).toString();
            logger.trace(String.format("Creating TarArchiveEntry: %s with linkName: %s", entryName, linkName));
            tarEntry = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK);
            tarEntry.setLinkName(linkName);
            logger.trace(String.format("Created TarArchiveEntry: %s; is symlink: %b: %s", tarEntry.getName(), tarEntry.isSymbolicLink(), tarEntry.getLinkName()));
        } else {
            tarEntry = new TarArchiveEntry(fileToAdd, entryName);
        }
        return tarEntry;
    }

    private String toAbsolute(String rootName, String entryName) throws IntegrationException {
        if (!entryName.startsWith(rootName)) {
            throw new IntegrationException(String.format(
                "Error converting entryName %s to absolute path for rootName %s: entryName should start with rootName",
                entryName,
                rootName
            ));
        }
        return entryName.substring(rootName.length());
    }

    private List<String> getListFromCommaSeparatedString(String containerFileSystemExcludedPathListString) {
        if (StringUtils.isBlank(containerFileSystemExcludedPathListString)) {
            return new ArrayList<>(0);
        }
        return Arrays.asList(containerFileSystemExcludedPathListString.split(","));
    }

    private List<String> cleanPaths(List<String> rawPaths) {
        List<String> cleanedPaths = new ArrayList<>(rawPaths.size());
        for (String rawPath : rawPaths) {
            String cleanedPath = rawPath.endsWith("/") ? removeFinalCharacter(rawPath) : rawPath;
            cleanedPaths.add(cleanedPath);
        }
        return cleanedPaths;
    }

    private String removeFinalCharacter(String rawPath) {
        return rawPath.substring(0, rawPath.length() - 1);
    }
}
