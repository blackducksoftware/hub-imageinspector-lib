package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class ContainerFileSystemOutputFileTest {

    @Test
    public void testCreateContainerFileSystemTarGz() throws IOException {

        final FileOperations fileOperations = new FileOperations();
        final File targetImageFileSystemRootDir = new File("test/workingDir");
        final String containerFileSystemOutputPath = "test/output/containerFileSys.tar.gz";

        ContainerFileSystemOutputFile.createContainerFileSystemTarGz(fileOperations, targetImageFileSystemRootDir, containerFileSystemOutputPath);

        final File containerFileSystemOutputFile = new File(containerFileSystemOutputPath);
        assertTrue(containerFileSystemOutputFile.exists());
    }

    @Test
    public void testCreateImageWrappedContainerFileSystemTar() throws IOException {

        final File targetImageFileSystemRootDir = new File("test/workingDir/imageFS");
        targetImageFileSystemRootDir.mkdirs();
        final File imageFile = new File(targetImageFileSystemRootDir, "imageFile.txt");
        imageFile.createNewFile();
        final String imageWrappedContainerFileSystemOutputPath = "test/output/imageWrappedContainerFileSys.tar.gz";

        ContainerFileSystemOutputFile.createImageWrappedContainerFileSystemTar(targetImageFileSystemRootDir, imageWrappedContainerFileSystemOutputPath);

        final File imageWrappedContainerFileSystemOutputFile = new File(imageWrappedContainerFileSystemOutputPath);
        assertTrue(imageWrappedContainerFileSystemOutputFile.exists());
    }
}
