/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.linux;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class TarOperations {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private FileOperations fileOperations;

    @Autowired
    public void setFileOperations(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    public File extractGzipTarToGivenDir(final File destinationDir, final File sourceTarFile) throws IOException {
        logPermissions(sourceTarFile);
        TarArchiveInputStream gzipTarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(sourceTarFile)));
        return extractTarToGivenDir(destinationDir, gzipTarArchiveInputStream);
    }

    public File extractTarToGivenDir(final File destinationDir, final File sourceTarFile) throws IOException {
        logPermissions(sourceTarFile);
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(sourceTarFile));
        return extractTarToGivenDir(destinationDir, tarArchiveInputStream);
    }

    private File extractTarToGivenDir(final File destinationDir, TarArchiveInputStream archiveInputStream) throws IOException {
        logger.debug(String.format("destinationDir: %s", destinationDir));
            TarArchiveEntry tarArchiveEntry = null;
            while (null != (tarArchiveEntry = archiveInputStream.getNextTarEntry())) {
                final File outputFile = new File(destinationDir, tarArchiveEntry.getName());
                if (tarArchiveEntry.isFile()) {
                    if (!outputFile.getParentFile().exists()) {
                        outputFile.getParentFile().mkdirs();
                    }
                    final OutputStream outputFileStream = new FileOutputStream(outputFile);
                    try {
                        logger.trace(String.format("Untarring %s", outputFile.getAbsolutePath()));
                        IOUtils.copy(archiveInputStream, outputFileStream);
                    } finally {
                        outputFileStream.close();
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
