package com.blackduck.integration.blackduck.imageinspector.linux;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LinuxFileSystemTest {

    @BeforeAll
    public static void setUpBeforeAll() throws Exception {
    }

    @AfterAll
    public static void tearDownAfterAll() throws Exception {
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
