/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.HardwiredManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.Executor;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.annotation.IntegrationTest;

@Category(IntegrationTest.class)
public class DockerTarParserTest {
    private final static int DPKG_STATUS_FILE_SIZE = 98016;

    private static final String IMAGE_NAME = "blackducksoftware/centos_minus_vim_plus_bacula";

    private static final String IMAGE_TAG = "1.0";

    private static final String LAYER_ID = "layerId1";

    @Test
    public void testExtractFullImage() throws IntegrationException, IOException {
        final File dockerTar = new File("build/images/test/centos_minus_vim_plus_bacula.tar");
        final File workingDirectory = TestUtils.createTempDirectory();
        System.out.println("workingDirectory: ${workingDirectory.getAbsolutePath()}");

        final DockerTarParser tarParser = new DockerTarParser();
        tarParser.setManifestFactory(new HardwiredManifestFactory());

        final List<File> layerTars = tarParser.extractLayerTars(workingDirectory, dockerTar);
        final List<ManifestLayerMapping> layerMappings = tarParser.getLayerMappings(workingDirectory, dockerTar.getName(), IMAGE_NAME, IMAGE_TAG);
        assertEquals(1, layerMappings.size());
        assertEquals(2, layerMappings.get(0).getLayers().size());
        final File imageFilesDir = tarParser.extractDockerLayers(workingDirectory, "imageName", "imageTag", layerTars, layerMappings);
        final ImageInfoParsed tarExtractionResults = tarParser.collectPkgMgrInfo(imageFilesDir);
        assertEquals("/var/lib/rpm", tarExtractionResults.getPkgMgr().getPackageManager().getDirectory());

        boolean varLibRpmNameFound = false;
        int numFilesFound = 0;
        final Collection<File> files = FileUtils.listFiles(workingDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        for (final File file : files) {
            numFilesFound++;
            if (file.getAbsolutePath().endsWith("var/lib/rpm/Name")) {
                System.out.println(file.getAbsolutePath());
                varLibRpmNameFound = true;
                final Executor e = new Executor();
                final String cmd = String.format("strings %s", file.getAbsolutePath());
                final String[] cmdOutput = e.executeCommand(cmd, 30000L);
                final String stringsOutput = Arrays.asList(cmdOutput).stream().collect(Collectors.joining("\n"));
                assertTrue(stringsOutput.contains("bacula-console"));
                assertTrue(stringsOutput.contains("bacula-client"));
                assertTrue(stringsOutput.contains("bacula-director"));
            }
        }
        assertTrue(varLibRpmNameFound);

        // MacOS file system does not preserve case which throws off the count
        System.out.println("Extracted ${numFilesFound} files");
        assertTrue(numFilesFound > 18000);
        assertTrue(numFilesFound < 19000);
    }

    @Test
    public void testExtractDockerLayerTarSimple() throws IOException {
        doLayerTest("simple");
    }

    private void doLayerTest(final String testFileDir) throws IOException {
        final File workingDirectory = TestUtils.createTempDirectory();
        final File tarExtractionDirectory = new File(workingDirectory, DockerTarParser.TAR_EXTRACTION_DIRECTORY);
        final File layerDir = new File(tarExtractionDirectory, String.format("ubuntu_latest.tar/%s", LAYER_ID));
        layerDir.mkdirs();
        final Path layerDirPath = Paths.get(layerDir.getAbsolutePath());
        assertEquals("layerId1", layerDirPath.getFileName().toString());

        final File dockerTar = new File(layerDir, "layer.tar");
        Files.copy(new File(String.format("src/test/resources/%s/layer.tar", testFileDir)).toPath(), dockerTar.toPath(), REPLACE_EXISTING);
        final List<File> layerTars = new ArrayList<>();
        layerTars.add(dockerTar);

        final DockerTarParser tarParser = new DockerTarParser();
        tarParser.setManifestFactory(new HardwiredManifestFactory());

        final List<ManifestLayerMapping> layerMappings = new ArrayList<>();
        final List<String> layerIds = new ArrayList<>();
        layerIds.add(LAYER_ID);
        final ManifestLayerMapping layerMapping = new ManifestLayerMapping(IMAGE_NAME, IMAGE_TAG, layerIds);
        layerMappings.add(layerMapping);

        final File targetImageFileSystemRootDir = tarParser.extractDockerLayers(workingDirectory, "blackducksoftware_centos_minus_vim_plus_bacula", "1.0", layerTars, layerMappings);
        assertEquals(tarExtractionDirectory.getAbsolutePath() + String.format("/imageFiles/%s", targetImageFileSystemRootDir.getName()), targetImageFileSystemRootDir.getAbsolutePath());

        final File dpkgStatusFile = new File(workingDirectory.getAbsolutePath() + String.format("/tarExtraction/imageFiles/%s/var/lib/dpkg/status", targetImageFileSystemRootDir.getName()));
        assertTrue(dpkgStatusFile.exists());

        assertEquals(DPKG_STATUS_FILE_SIZE, FileUtils.sizeOf(dpkgStatusFile));
    }
}
