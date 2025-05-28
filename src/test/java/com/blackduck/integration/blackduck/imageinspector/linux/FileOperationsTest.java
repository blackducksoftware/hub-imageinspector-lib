package com.blackduck.integration.blackduck.imageinspector.linux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class FileOperationsTest {
    private static final File TEST_DIR = new File("test");

    @BeforeEach
    public void setUp() throws IOException {
        deleteDirectory(TEST_DIR);
        TEST_DIR.mkdirs();
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    private static FileOperations fileOperations;

    @BeforeAll
    public static void setUpBeforeAll() {
        fileOperations = new FileOperations();
    }

    @AfterAll
    public static void tearDownAfterAll() {
    }

    @Test
    public void testMoveFile() throws IOException {
        final File fileToMove = new File(TEST_DIR, "fileToMove.txt");
        fileToMove.createNewFile();
        File imageDir = new File(TEST_DIR, "output");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        final File destinationFile = new File(imageDir, "fileToMove.txt");
        destinationFile.delete();

        fileOperations.moveFile(fileToMove, imageDir);
        assertTrue(destinationFile.exists());
    }

    @Test
    public void testDeleteFilesOnly() throws IOException {
        final File fileToDelete = new File(TEST_DIR, "dirWithFileToDelete/fileToDelete.txt");
        fileToDelete.getParentFile().mkdir();
        fileToDelete.createNewFile();

        fileOperations.deleteFilesOnly(fileToDelete.getParentFile());

        assertFalse(fileToDelete.exists());
        assertTrue(fileToDelete.getParentFile().exists());
    }


    @Test
    public void testGetFileOwnerGroupPermsMsgs() throws IOException {
        final File fileToLog = new File(TEST_DIR, "dirWithFileToDelete/fileToDelete.txt");
        fileToLog.getParentFile().mkdir();
        fileToLog.createNewFile();

        final List<String> msgs = fileOperations.getFileOwnerGroupPermsMsgs(fileToLog);

        assertEquals(2, msgs.size());
        assertTrue(msgs.get(0).startsWith("Current process owner:"));
        assertTrue(msgs.get(1).contains("owner:"));
        assertTrue(msgs.get(1).contains("group:"));
        assertTrue(msgs.get(1).contains("perms:"));
    }

    @Test
    public void testDeleteDirPersistently() throws IOException, InterruptedException {
        final File fileToDelete = new File(TEST_DIR, "dirWithFileToDelete/fileToDelete.txt");
        fileToDelete.getParentFile().mkdir();
        fileToDelete.createNewFile();

        fileOperations.deleteDirPersistently(fileToDelete.getParentFile());

        assertFalse(fileToDelete.exists());
        assertFalse(fileToDelete.getParentFile().exists());
    }

    @Test
    public void testCreateTempDirectory() throws IOException {
        final File tempDir = fileOperations.createTempDirectory(true);
        assertTrue(tempDir.isDirectory());
        assertTrue(tempDir.canWrite());
        assertEquals(0, tempDir.listFiles().length);
        tempDir.deleteOnExit();
    }

    @Test
    public void testPruneProblematicSymLinksRecursively_danglingLink() throws IOException {
        File testDir = new File(TEST_DIR, "symlinks");
        testDir.mkdirs();
        File danglingLink = new File(testDir, "danglingLink");
        Files.createSymbolicLink(danglingLink.toPath(), new File("nonexistent").toPath());

        fileOperations.pruneProblematicSymLinksRecursively(testDir);

        assertFalse(danglingLink.exists());
    }

    @Test
    public void testPruneProblematicSymLinksRecursively_circularLink() throws IOException {
        File testDir = new File(TEST_DIR, "symlinks");
        testDir.mkdirs();
        File circularLink = new File(testDir, "circularLink");
        Files.createSymbolicLink(circularLink.toPath(), Path.of(testDir.getAbsolutePath()));

        fileOperations.pruneProblematicSymLinksRecursively(testDir);

        assertFalse(circularLink.exists());
    }

    @Test
    public void testPruneProblematicSymLinksRecursively_validLink() throws IOException {
        File testDir = new File(TEST_DIR, "symlinks");
        testDir.mkdirs();
        File targetFile = new File(testDir, "targetFile");
        targetFile.createNewFile();
        File validLink = new File(testDir, "validLink");
        Files.createSymbolicLink(validLink.toPath(), Path.of("targetFile"));

        assertTrue(targetFile.exists());
        assertTrue(validLink.exists());
    }

    @Test
    public void testPruneProblematicSymLinksRecursively_nestedDirectories() throws IOException {
        File testDir = new File(TEST_DIR, "symlinks/nested");
        testDir.mkdirs();

        File normalFile = new File(testDir, "normalFile");
        normalFile.createNewFile();

        File danglingLink = new File(testDir, "danglingLink");
        Files.createSymbolicLink(danglingLink.toPath(), new File("nonexistent").toPath());

        fileOperations.pruneProblematicSymLinksRecursively(new File(TEST_DIR, "symlinks"));

        assertFalse(danglingLink.exists());
        assertTrue(normalFile.exists());
    }

    @Test
    public void testPruneProblematicSymLinksRecursively_symlinkToDir_absolutePath() throws IOException {
        File testDir = new File(TEST_DIR, "symlinks/nested");
        testDir.mkdirs();

        File normalFile = new File(testDir, "normalFile");
        normalFile.createNewFile();

        File linkToDir = new File(testDir.getParent(), "linkToDir");
        Files.createSymbolicLink(linkToDir.toPath(), Path.of(testDir.getAbsolutePath()));

        assertTrue(linkToDir.exists());

        fileOperations.pruneProblematicSymLinksRecursively(new File(TEST_DIR, "symlinks"));

        assertFalse(linkToDir.exists());
        assertTrue(testDir.exists());
        assertTrue(normalFile.exists());
    }

    @Test
    public void testPruneProblematicSymLinksRecursively_symlinkToDir_relativePath() throws IOException {
        File testDir = new File(TEST_DIR, "symlinks/nested");
        testDir.mkdirs();

        File normalFile = new File(testDir, "normalFile");
        normalFile.createNewFile();

        File linkToDir = new File(testDir.getParent(), "linkToDir");
        Files.createSymbolicLink(linkToDir.toPath(), Path.of("nested"));

        assertTrue(linkToDir.exists());

        fileOperations.pruneProblematicSymLinksRecursively(new File(TEST_DIR, "symlinks"));

        assertFalse(linkToDir.exists());
        assertTrue(testDir.exists());
        assertTrue(normalFile.exists());
    }
}
