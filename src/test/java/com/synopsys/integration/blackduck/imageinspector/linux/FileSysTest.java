package com.synopsys.integration.blackduck.imageinspector.linux;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.compress.compressors.CompressorException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileSysTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws CompressorException, IOException {
        final LinuxFileSystem fSys = new LinuxFileSystem(new File("src/test/resources/imageDir"), new FileOperations());
        final File outputTarFile = new File("test/containerFileSystem.tar.gz");
        outputTarFile.delete();
        assertFalse(outputTarFile.exists());
        fSys.writeToTarGz(outputTarFile);
        assertTrue(outputTarFile.exists());
    }

}
