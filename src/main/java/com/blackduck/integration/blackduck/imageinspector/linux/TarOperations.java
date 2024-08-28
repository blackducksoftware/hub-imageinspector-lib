/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.linux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.blackduck.integration.blackduck.imageinspector.api.InvalidArchiveFormatException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TarOperations {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private FileOperations fileOperations;

    @Autowired
    public void setFileOperations(FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    public File extractTarToGivenDir(File destinationDir, File sourceTarFile) throws InvalidArchiveFormatException, IOException {
        logPermissions(sourceTarFile);
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(sourceTarFile));
        return extractTarToGivenDir(destinationDir, tarArchiveInputStream, sourceTarFile.getAbsolutePath(), "UNIX tar");
    }

    public File extractGzipTarToGivenDir(File destinationDir, File sourceTarFile) throws InvalidArchiveFormatException, IOException {
        logPermissions(sourceTarFile);
        TarArchiveInputStream gzipTarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(sourceTarFile)));
        return extractTarToGivenDir(destinationDir, gzipTarArchiveInputStream, sourceTarFile.getAbsolutePath(), "GNU gzip compressed tar");
    }

    public File extractZstdTarToGivenDir(File destinationDir, File sourceTarFile) throws InvalidArchiveFormatException, IOException {
        logPermissions(sourceTarFile);
        TarArchiveInputStream gzipTarArchiveInputStream = new TarArchiveInputStream(new ZstdCompressorInputStream(new FileInputStream(sourceTarFile)));
        return extractTarToGivenDir(destinationDir, gzipTarArchiveInputStream, sourceTarFile.getAbsolutePath(), "Zstd compressed tar");
    }

    private File extractTarToGivenDir(File destinationDir, TarArchiveInputStream archiveInputStream, String archiveFilePath, String archiveType)
        throws InvalidArchiveFormatException {
        logger.debug("Extracting %s archive %s", archiveType, archiveFilePath);
        try {
            return extractTarToGivenDir(destinationDir, archiveInputStream);
        } catch (IOException e) {
            String msg = String.format("Archive file %s is not a valid %s archive", archiveFilePath, archiveType);
            logger.error(msg);
            throw new InvalidArchiveFormatException(msg);
        }
    }

    //ac- TODO- is DI going to use this instead of DockerLayerTarExtractor?
    private File extractTarToGivenDir(File destinationDir, TarArchiveInputStream archiveInputStream) throws IOException {
        logger.debug("destinationDir: {}", destinationDir.getAbsolutePath());
        TarArchiveEntry tarArchiveEntry;
        while (null != (tarArchiveEntry = archiveInputStream.getNextTarEntry())) {
            File outputFile = new File(destinationDir, tarArchiveEntry.getName());
            if (tarArchiveEntry.isFile()) {
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                try (OutputStream outputFileStream = new FileOutputStream(outputFile)) {
                    logger.trace("Untarring {}", outputFile.getAbsolutePath());
                    IOUtils.copy(archiveInputStream, outputFileStream);
                }
            }
        }
        return destinationDir;
    }

    private void logPermissions(File file) {
        fileOperations.logFileOwnerGroupPerms(file.getParentFile());
        fileOperations.logFileOwnerGroupPerms(file);
    }
}
