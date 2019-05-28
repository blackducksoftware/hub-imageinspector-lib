package com.synopsys.integration.blackduck.imageinspector.lib;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;

public class ContainerFileSystemOutputFile {
    private static final Logger logger = LoggerFactory.getLogger(ContainerFileSystemOutputFile.class);

    public static void createContainerFileSystemTarGz(final FileOperations fileOperations, final File targetImageFileSystemRootDir, final String containerFileSystemOutputPath) throws IOException {
        logger.info("Including container file system in output");
        final File outputDirectory = new File(containerFileSystemOutputPath);
        final File containerFileSystemTarFile = new File(containerFileSystemOutputPath);
        logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
        final LinuxFileSystem containerFileSys = new LinuxFileSystem(targetImageFileSystemRootDir, fileOperations);
        containerFileSys.writeToTarGz(containerFileSystemTarFile);
    }

    public static void createImageWrappedContainerFileSystemTar(final File targetImageFileSystemRootDir, final String imageWrappedContainerFileSystemOutputPath) {

    }
}
