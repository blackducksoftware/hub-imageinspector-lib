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
package com.blackducksoftware.integration.hub.imageinspector.linux;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.lib.PackageManagerEnum;

public class FileSys {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final int LIB_MAX_DEPTH = 4;
    private static final int ETC_MAX_DEPTH = 0;
    private final File root;

    public FileSys(final File root) {
        this.root = root;
    }

    public Set<PackageManagerEnum> getPackageManagers() {
        final Set<PackageManagerEnum> packageManagers = new HashSet<>();

        logger.debug(String.format("Looking in root dir %s for lib dir", root.getAbsolutePath()));
        final List<File> libDirs = FileOperations.findDirWithName(LIB_MAX_DEPTH, root, "lib");
        if (libDirs != null) {
            for (final File libDir : libDirs) {
                for (final File packageManagerDirectory : libDir.listFiles()) {
                    logger.trace(String.format("Checking dir %s to see if it's a package manager dir", packageManagerDirectory.getAbsolutePath()));
                    try {
                        // TODO this is too simplistic IDOCKER-363
                        logger.trace(String.format("Found a lib dir: %s", packageManagerDirectory.getAbsolutePath()));
                        packageManagers.add(PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName()));
                    } catch (final IllegalArgumentException e) {
                        logger.trace(String.format("%s is not a package manager", packageManagerDirectory.getName()));
                    }
                }
            }
        }
        return packageManagers;
    }

    public Optional<OperatingSystemEnum> getOperatingSystem() {
        logger.debug(String.format("Looking in root dir %s for etc dir", root.getAbsolutePath()));
        final List<File> etcDirs = FileOperations.findDirWithName(ETC_MAX_DEPTH, root, "etc");
        if (etcDirs.size() != 1) {
            logger.warn(String.format("Unable to determine image OS: Unable to find /etc dir in %s", root.getAbsolutePath()));
            return Optional.empty();
        }
        File distroDetailsFile;
        final Optional<File> lsbReleaseFile = FileOperations.findFileWithName(etcDirs.get(0), "lsb-release");
        String distroNameKey;
        Optional<File> osReleaseFile;
        if (lsbReleaseFile.isPresent()) {
            distroDetailsFile = lsbReleaseFile.get();
            distroNameKey = "DISTRIB_ID";
        } else {
            osReleaseFile = FileOperations.findFileWithName(etcDirs.get(0), "os-release");
            if (osReleaseFile.isPresent()) {
                distroDetailsFile = osReleaseFile.get();
                distroNameKey = "ID";
            } else {
                return Optional.empty();
            }
        }
        String distroName;
        try {
            distroName = getDistroNameFromDistroDetailsFile(distroDetailsFile, distroNameKey);
        } catch (final Exception distroNameParseException) {
            logger.warn(String.format("Unable to read operating system name from file %s: %s", distroDetailsFile.getAbsolutePath(),
                    distroNameParseException.getMessage()));
            return Optional.empty();
        }
        OperatingSystemEnum distro;
        try {
            distro = OperatingSystemEnum.determineOperatingSystem(distroName);
        } catch (final IllegalArgumentException distroNameConversionException) {
            logger.warn(String.format("Unrecognized operating system name (%s) read from file %s. Error: %s", distroName, distroDetailsFile.getAbsolutePath(), distroNameConversionException.getMessage()));
            return Optional.empty();
        }
        return Optional.of(distro);
    }

    private String getDistroNameFromDistroDetailsFile(final File distroDetailsFile, final String distroNameKey) throws IOException, IntegrationException {
        final String targetPrefix = String.format("%s=", distroNameKey);
        final List<String> lines = FileUtils.readLines(distroDetailsFile, StandardCharsets.UTF_8);
        for (final String line : lines) {
            if (line.startsWith(targetPrefix)) {
                if (line.length() == targetPrefix.length()) {
                    throw new IntegrationException(String.format("Distro name in file %s is empty", distroDetailsFile.getAbsolutePath()));
                }
                return line.substring(targetPrefix.length());
            }
        }
        throw new IntegrationException(String.format("Distro name not found in file %s", distroDetailsFile.getAbsolutePath()));
    }

    public void createTarGz(final File outputTarFile) throws CompressorException, IOException {
        outputTarFile.getParentFile().mkdirs();
        FileOperations.logFileOwnerGroupPerms(outputTarFile.getParentFile());
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

    private void addFileToTar(final TarArchiveOutputStream tOut, final File fileToAdd, final String base) throws IOException {
        final String entryName = base + fileToAdd.getName();

        TarArchiveEntry tarEntry = null;
        if (Files.isSymbolicLink(fileToAdd.toPath())) {
            tarEntry = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK);
            tarEntry.setLinkName(Files.readSymbolicLink(fileToAdd.toPath()).toString());
        } else {
            tarEntry = new TarArchiveEntry(fileToAdd, entryName);
        }
        tOut.putArchiveEntry(tarEntry);

        if (Files.isSymbolicLink(fileToAdd.toPath())) {
            tOut.closeArchiveEntry();
        } else if (fileToAdd.isFile()) {
            try (final InputStream fileToAddInputStream = new FileInputStream(fileToAdd)) {
                IOUtils.copy(fileToAddInputStream, tOut);
            }
            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();
            final File[] children = fileToAdd.listFiles();
            if (children != null) {
                for (final File child : children) {
                    logger.trace(String.format("Adding to tar.gz file: %s", child.getAbsolutePath()));
                    addFileToTar(tOut, child, entryName + "/");
                }
            }
        }
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
