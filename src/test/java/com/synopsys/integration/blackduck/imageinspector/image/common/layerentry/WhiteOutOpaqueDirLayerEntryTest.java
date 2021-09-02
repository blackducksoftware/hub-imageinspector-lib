package com.synopsys.integration.blackduck.imageinspector.image.common.layerentry;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WhiteOutOpaqueDirLayerEntryTest {

    @TempDir
    File tempDir;

    @Test
    void test() {
        TarArchiveEntry layerEntry = Mockito.mock(TarArchiveEntry.class);
        Mockito.when(layerEntry.getName()).thenReturn(".wh..wh..opq");
        File layerOutputDir = tempDir;
        LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();
        WhiteOutOpaqueDirLayerEntry whiteOutOpaqueDirLayerEntry = new WhiteOutOpaqueDirLayerEntry(layerEntry, layerOutputDir, fileDeleter);

        List<File> filesToDelete = whiteOutOpaqueDirLayerEntry.processFiles();

        assertTrue(filesToDelete.isEmpty());
        assertTrue(layerOutputDir.isDirectory());
        assertEquals(0, layerOutputDir.listFiles().length);
    }
}
