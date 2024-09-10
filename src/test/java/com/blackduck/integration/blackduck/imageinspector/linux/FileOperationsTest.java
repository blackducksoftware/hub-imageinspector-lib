package com.blackduck.integration.blackduck.imageinspector.linux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FileOperationsTest {
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
        final File fileToMove = new File("test/fileToMove.txt");
        fileToMove.createNewFile();
        File imageDir = new File("test/output");
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
        final File fileToDelete = new File("test/dirWithFileToDelete/fileToDelete.txt");
        fileToDelete.getParentFile().mkdir();
        fileToDelete.createNewFile();

        fileOperations.deleteFilesOnly(fileToDelete.getParentFile());

        assertFalse(fileToDelete.exists());
        assertTrue(fileToDelete.getParentFile().exists());
    }


    @Test
    public void testGetFileOwnerGroupPermsMsgs() throws IOException {
        final File fileToLog = new File("test/dirWithFileToDelete/fileToDelete.txt");
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
        final File fileToDelete = new File("test/dirWithFileToDelete/fileToDelete.txt");
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
}
