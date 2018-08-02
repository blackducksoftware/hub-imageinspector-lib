package com.blackducksoftware.integration.hub.imageinspector.linux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.compress.compressors.CompressorException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;

public class FileSysTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testCreateTarGz() throws CompressorException, IOException {
        final FileSys fSys = new FileSys(new File("src/test/resources/imageDir"));
        final File outputTarFile = new File("test/containerFileSystem.tar.gz");
        outputTarFile.delete();
        assertFalse(outputTarFile.exists());
        fSys.createTarGz(outputTarFile);
        assertTrue(outputTarFile.exists());
    }

    @Test
    public void testGetOperatingSystemLsbRelease() {
        final FileSys fSys = new FileSys(new File("src/test/resources/lsbReleaseOs"));
        assertEquals(OperatingSystemEnum.UBUNTU, fSys.getOperatingSystem().get());
    }

    @Test
    public void testGetOperatingSystemOsRelease() {
        final FileSys fSys = new FileSys(new File("src/test/resources/osReleaseOs"));
        assertEquals(OperatingSystemEnum.UBUNTU, fSys.getOperatingSystem().get());
    }

    @Test
    public void testGetOperatingSystemOsReleaseMissingId() {
        final FileSys fSys = new FileSys(new File("src/test/resources/missingReleaseOs"));
        assertFalse(fSys.getOperatingSystem().isPresent());
    }

    @Test
    public void testGetOperatingSystemOsReleaseUnrecognizedId() {
        final FileSys fSys = new FileSys(new File("src/test/resources/badReleaseOs"));
        assertFalse(fSys.getOperatingSystem().isPresent());
    }
}
