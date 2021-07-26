package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ImageLayerArchiveExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public class ImageLayerMetadataParserExtractorIntTest {

    @Test
    public void testOpaqueDir() throws IOException {
        final File tarFile = new File("src/test/resources/layers/whiteoutOpaqueDir/layer.tar");
        final File outputDir = new File("test/output/whiteoutOpaqueDirLayer");
        outputDir.mkdirs();
        FileUtils.deleteQuietly(outputDir);
        outputDir.mkdirs();
        final File dirContainingFileThatShouldBeRemoved = new File(outputDir, "opt/luciddg-server/modules");
        final File fileThatShouldBeRemoved = new File(dirContainingFileThatShouldBeRemoved, "SHOULDBEREMOVED.txt");
        final File dirContainingAnotherFileThatShouldBeRemoved = new File(outputDir, "opt/luciddg-server/modules/django/bin");
        final File anotherFileThatShouldBeRemoved = new File(dirContainingAnotherFileThatShouldBeRemoved, "SHOULDBEREMOVED.txt");

        dirContainingFileThatShouldBeRemoved.mkdirs();
        fileThatShouldBeRemoved.createNewFile();

        dirContainingAnotherFileThatShouldBeRemoved.mkdirs();
        anotherFileThatShouldBeRemoved.createNewFile();

        final ImageLayerArchiveExtractor imageLayerArchiveExtractor = new ImageLayerArchiveExtractor();
        final List<File> filesToRemove = imageLayerArchiveExtractor.extractLayerTarToDir(new FileOperations(), tarFile, outputDir);

        assertEquals(0, filesToRemove.size());
        final File fileThatShouldBeCreated = new File(outputDir, "opt/luciddg-server/modules/django/bin/100_assets.csv");
        assertFalse(fileThatShouldBeRemoved.exists());
        assertFalse(anotherFileThatShouldBeRemoved.exists());
        assertTrue(fileThatShouldBeCreated.exists());
    }

    @Test
    public void testFilesInOpaqueLayerAddedToOutput() throws IOException {
        File tarFile = new File("src/test/resources/layers/eapLayer/layer.tar");
        File outputDir = new File("test/output/eapLayer");
        outputDir.mkdirs();
        FileUtils.deleteQuietly(outputDir);
        outputDir.mkdirs();

        File partitionDir = new File(outputDir, "opt/partition");
        File fileThatShouldExist1 = new File(partitionDir, "logging.properties");
        File fileThatShouldExist2 = new File(partitionDir, "osquery.py");
        File fileThatShouldExist3 = new File(partitionDir, "queryosapi.py");
        File fileThatShouldExist4 = new File(partitionDir, "partitionPV.sh");

        ImageLayerArchiveExtractor imageLayerArchiveExtractor = new ImageLayerArchiveExtractor();
        TarArchiveInputStream inputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarFile)), "UTF-8");
        List<File> filesToRemove = imageLayerArchiveExtractor.extractLayerTarToDir(new FileOperations(), inputStream, outputDir);

        assertTrue(filesToRemove.isEmpty());
        assertTrue(fileThatShouldExist1.exists());
        assertTrue(fileThatShouldExist2.exists());
        assertTrue(fileThatShouldExist3.exists());
        assertTrue(fileThatShouldExist4.exists());
    }
}
