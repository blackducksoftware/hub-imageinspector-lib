package com.synopsys.integration.blackduck.imageinspector.image.common.archive;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageLayerArchiveExtractorTest {

    @TempDir
    File tempDir;

    @Test
    void test() throws IOException {
        ImageLayerArchiveExtractor imageLayerArchiveExtractor = new ImageLayerArchiveExtractor();
        FileOperations fileOperations = new FileOperations();
        File tarFile = new File("src/test/resources/omitdir/layer.tar");
        File outputDir = new File(tempDir, "output");

        imageLayerArchiveExtractor.extractLayerTarToDir(fileOperations, tarFile, outputDir);

        List<File> extractedFiles = Arrays.asList(outputDir.listFiles());
        assertEquals(2, extractedFiles.size());
        assertTrue(extractedFiles.stream().map(File::getName).filter(name -> name.equals("omit")).findAny().isPresent());
        assertTrue(extractedFiles.stream().map(File::getName).filter(name -> name.equals("regular")).findAny().isPresent());
        assertTrue(extractedFiles.stream().findFirst().get().isDirectory());
    }
}
