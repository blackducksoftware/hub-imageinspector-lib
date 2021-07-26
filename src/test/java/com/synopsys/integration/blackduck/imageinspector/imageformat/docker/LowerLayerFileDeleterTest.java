package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.layerentry.LowerLayerFileDeleter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LowerLayerFileDeleterTest {
    @Test
    public void testDoesNotFollowSymbolicLinks() throws IOException {
        File parentOFfSymLink = new File("test/parentOfSymLink");
        parentOFfSymLink.mkdir();

        File symLinkTarget = new File(parentOFfSymLink, "symLink");
        if (symLinkTarget.exists()) {
            FileUtils.deleteQuietly(symLinkTarget);
        }

        File dirWithFileNotToDelete = new File("test/dirWithFileNotToDelete");
        dirWithFileNotToDelete.mkdir();
        File fileNotToDelete = new File(dirWithFileNotToDelete, "fileNotToDelete.txt");
        fileNotToDelete.createNewFile();

        createSymbolicLink(symLinkTarget, dirWithFileNotToDelete);
        Assertions.assertTrue(symLinkExists(parentOFfSymLink));

        LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();
        fileDeleter.addFilesAddedByCurrentLayer(Arrays.asList(parentOFfSymLink.getAbsolutePath(), symLinkTarget.getAbsolutePath()));

        fileDeleter.deleteFilesAddedByLowerLayers(parentOFfSymLink);
        // make sure file deleter didn't follow symLink to dirWithFileNotToDelete
        Assertions.assertTrue(fileNotToDelete.exists());
    }

    private void createSymbolicLink(File fileToLink, File target) throws IOException {
        Path targetPath = target.toPath();
        targetPath = targetPath.toAbsolutePath();

        Path link = fileToLink.toPath();
        link = link.toAbsolutePath();

        Files.createSymbolicLink(link, targetPath);
    }

    private boolean symLinkExists(File file) {
        File[] children = file.listFiles();
        if (children == null || children.length == 0) {
            return false;
        }
        boolean foundSymLink = false;
        for (File child : children) {
            if (Files.isSymbolicLink(child.toPath())) {
                return true;
            }
            foundSymLink = foundSymLink || symLinkExists(child);
        }
        return foundSymLink;
    }
}
