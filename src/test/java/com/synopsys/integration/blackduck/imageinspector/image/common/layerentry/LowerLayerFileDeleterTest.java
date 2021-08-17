package com.synopsys.integration.blackduck.imageinspector.image.common.layerentry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LowerLayerFileDeleterTest {

    @TempDir
    File tempDir;

    @Test
    void test() throws IOException {

        File oldFile = new File(tempDir, "oldFile");
        File newFile = new File(tempDir, "newFile");
        oldFile.createNewFile();
        newFile.createNewFile();

        LowerLayerFileDeleter lowerLayerFileDeleter = new LowerLayerFileDeleter();
        lowerLayerFileDeleter.addFilesAddedByCurrentLayer(Arrays.asList(newFile.getName()));
        lowerLayerFileDeleter.deleteFilesAddedByLowerLayers(tempDir);

        assertTrue(newFile.exists());
        assertFalse(oldFile.exists());
    }
}
