package com.synopsys.integration.blackduck.imageinspector.linux;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileCompression {
    private static final Logger logger = LoggerFactory.getLogger(FileCompression.class);

    public static void writeDirToTarGz(final FileOperations fileOperations, final File dir, final File outputTarGzFile) throws IOException {
        outputTarGzFile.getParentFile().mkdirs();
        fileOperations.logFileOwnerGroupPerms(outputTarGzFile.getParentFile());
        FileOutputStream fOut = null;
        BufferedOutputStream bOut = null;
        GzipCompressorOutputStream gzOut = null;
        TarArchiveOutputStream tOut = null;
        try {
            fOut = new FileOutputStream(outputTarGzFile);
            bOut = new BufferedOutputStream(fOut);
            gzOut = new GzipCompressorOutputStream(bOut);
            tOut = new TarArchiveOutputStream(gzOut);
            tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            addFileToTar(tOut, dir, "");
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

    private static void addFileToTar(final TarArchiveOutputStream tOut, final File fileToAdd, final String base) throws IOException {
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
}
