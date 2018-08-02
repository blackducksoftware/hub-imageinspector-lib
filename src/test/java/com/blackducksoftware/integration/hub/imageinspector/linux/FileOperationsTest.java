package com.blackducksoftware.integration.hub.imageinspector.linux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileOperationsTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testMoveFile() throws IOException {
        final File fileToMove = new File("test/fileToMove.txt");
        fileToMove.createNewFile();
        final File destinationDir = new File("test/output");
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        final File destinationFile = new File(destinationDir, "fileToMove.txt");
        destinationFile.delete();
        FileOperations.moveFile(fileToMove, destinationDir);
        assertTrue(destinationFile.exists());
    }

    @Test
    public void testFindFile() throws IOException {
        final Optional<File> foundFile = FileOperations.findFileWithName(new File("src/test/resources"), "test.txt");
        assertTrue(foundFile.isPresent());
        assertTrue(foundFile.get().isFile());
        assertEquals("test.txt", foundFile.get().getName());
        assertTrue(FileUtils.readFileToString(foundFile.get(), StandardCharsets.UTF_8).contains("This is a test"));
    }

    @Test
    public void testFindDir() throws IOException {
        final List<File> foundDirs = FileOperations.findDirWithName(3, new File("src/test/resources"), "subdir");
        assertEquals(1, foundDirs.size());
        assertTrue(foundDirs.get(0).isDirectory());
        assertEquals("subdir", foundDirs.get(0).getName());
    }
}
