package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public class DockerLayerTarExtractorIntTest {

    @Test
    public void test() throws IOException {
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

        final DockerLayerTarExtractor dockerLayerTarExtractor = new DockerLayerTarExtractor();
        final List<File> filesToRemove = dockerLayerTarExtractor.extractLayerTarToDir(tarFile, outputDir);

        final File fileThatShouldBeCreated = new File(outputDir, "opt/luciddg-server/modules/django/bin/100_assets.csv");
//        assertFalse(fileThatShouldBeRemoved.exists());
//        assertFalse(anotherFileThatShouldBeRemoved.exists());
//        assertTrue(fileThatShouldBeCreated.exists());
        System.out.println("Done");
    }
}
