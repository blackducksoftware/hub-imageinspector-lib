package com.synopsys.integration.blackduck.imageinspector.linux;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LinuxFileSysTest {

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testWriteToTarGz() throws IOException {
        final LinuxFileSystem fSys = new LinuxFileSystem(new File("src/test/resources/imageDir"), new FileOperations());
        final File outputTarFile = new File("test/containerFileSystem.tar.gz");
        outputTarFile.delete();
        assertFalse(outputTarFile.exists());
        fSys.writeToTarGz(outputTarFile);
        assertTrue(outputTarFile.exists());
    }
}
