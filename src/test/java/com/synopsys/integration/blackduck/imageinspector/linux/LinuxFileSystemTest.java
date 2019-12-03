package com.synopsys.integration.blackduck.imageinspector.linux;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LinuxFileSystemTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testWriteToTarGz() throws IOException {
        final LinuxFileSystem fSys = new LinuxFileSystem(new File("src/test/resources/imageDir"), new FileOperations());
        final File outputTarFile = new File("test/containerFileSystem.tar.gz");
        outputTarFile.delete();
        assertFalse(outputTarFile.exists());
        fSys.writeToTarGz(outputTarFile, null);
        assertTrue(outputTarFile.exists());
        assertTrue(outputTarFile.length() > 800L);
    }

    @Test
    public void testWriteToTarGzWithExclusions() throws IOException {
        final LinuxFileSystem fSys = new LinuxFileSystem(new File("src/test/resources/imageDir"), new FileOperations());
        final File outputTarFile = new File("test/containerFileSystem.tar.gz");
        outputTarFile.delete();
        assertFalse(outputTarFile.exists());
        fSys.writeToTarGz(outputTarFile, "/ubuntu,/centos");
        assertTrue(outputTarFile.exists());
        System.out.printf("Size of tar.gz file: %d\n", outputTarFile.length());
        assertTrue(outputTarFile.length() < 1000L);
    }

    @Test
    public void testWriteToTarGzWithExclusionsWithTrailingSlash() throws IOException {
        final LinuxFileSystem fSys = new LinuxFileSystem(new File("src/test/resources/imageDir"), new FileOperations());
        final File outputTarFile = new File("test/containerFileSystem.tar.gz");
        outputTarFile.delete();
        assertFalse(outputTarFile.exists());
        fSys.writeToTarGz(outputTarFile, "/ubuntu/,/centos/");
        assertTrue(outputTarFile.exists());
        System.out.printf("Size of tar.gz file: %d\n", outputTarFile.length());
        assertTrue(outputTarFile.length() < 1000L);
    }
}
