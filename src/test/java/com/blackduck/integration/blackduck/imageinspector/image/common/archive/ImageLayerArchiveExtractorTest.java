package com.blackduck.integration.blackduck.imageinspector.image.common.archive;

import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ImageLayerArchiveExtractorTest {

    @TempDir
    File tempDir;

    // TODO next 3 tests could be parameterized

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

    @Test
    void testTarGz() throws IOException {
        ImageLayerArchiveExtractor imageLayerArchiveExtractor = new ImageLayerArchiveExtractor();
        FileOperations fileOperations = new FileOperations();
        File tarFile = new File("src/test/resources/layers/targz/123456");
        File outputDir = new File(tempDir, "output");

        imageLayerArchiveExtractor.extractLayerGzipTarToDir(fileOperations, tarFile, outputDir);

        List<File> extractedFiles = Arrays.asList(outputDir.listFiles());
        assertEquals(2, extractedFiles.size());
        assertTrue(extractedFiles.stream().map(File::getName).filter(name -> name.equals("omit")).findAny().isPresent());
        assertTrue(extractedFiles.stream().map(File::getName).filter(name -> name.equals("regular")).findAny().isPresent());
        assertTrue(extractedFiles.stream().findFirst().get().isDirectory());
    }

    @Test
    void testTarZstd() throws IOException {
        ImageLayerArchiveExtractor imageLayerArchiveExtractor = new ImageLayerArchiveExtractor();
        FileOperations fileOperations = new FileOperations();
        File tarFile = new File("src/test/resources/layers/zstd/123456");
        File outputDir = new File(tempDir, "output");

        imageLayerArchiveExtractor.extractLayerZstdTarToDir(fileOperations, tarFile, outputDir);

        List<File> extractedFiles = Arrays.asList(outputDir.listFiles());
        assertEquals(2, extractedFiles.size());
        assertTrue(extractedFiles.stream().map(File::getName).filter(name -> name.equals("omit")).findAny().isPresent());
        assertTrue(extractedFiles.stream().map(File::getName).filter(name -> name.equals("regular")).findAny().isPresent());
        assertTrue(extractedFiles.stream().findFirst().get().isDirectory());
    }

    @Test
    public void testOpaqueDir() throws IOException {
        final File tarFile = new File("src/test/resources/layers/whiteoutOpaqueDir/layer.tar");
        final File outputDir = new File(tempDir, "whiteoutOpaqueDirLayer");
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
        File outputDir = new File(tempDir, "eapLayer");
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
