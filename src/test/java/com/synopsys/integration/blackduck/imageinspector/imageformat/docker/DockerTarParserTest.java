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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.api.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.Executor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.exception.IntegrationException;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class DockerTarParserTest {
    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private final static int DPKG_STATUS_FILE_SIZE = 98016;

    private static final String IMAGE_NAME = "blackducksoftware/centos_minus_vim_plus_bacula";

    private static final String IMAGE_TAG = "1.0";

    private static final String LAYER_ID = "layerId1";

    @Test
    public void testExtractFullImage() throws IntegrationException, IOException {
        final File dockerTar = new File("build/images/test/centos_minus_vim_plus_bacula.tar");
        final File workingDirectory = TestUtils.createTempDirectory();
        final File tarExtractionDirectory = new File(workingDirectory, DockerTarParser.TAR_EXTRACTION_DIRECTORY);
        System.out.println("workingDirectory: ${workingDirectory.getAbsolutePath()}");

        final DockerTarParser tarParser = new DockerTarParser();
        tarParser.setManifestFactory(new ManifestFactory());
        tarParser.setOs(new Os());

        final List<File> layerTars = tarParser.extractLayerTars(workingDirectory, dockerTar);
        final ManifestLayerMapping layerMapping = tarParser.getLayerMapping(new GsonBuilder(), workingDirectory, dockerTar.getName(), IMAGE_NAME, IMAGE_TAG);
        assertEquals(2, layerMapping.getLayers().size());
        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(IMAGE_NAME, IMAGE_TAG));
        final ComponentExtractorFactory componentExtractorFactory = new ComponentExtractorFactory();
        tarParser.extractDockerLayers(new Gson(), componentExtractorFactory, OperatingSystemEnum.CENTOS, new ImageComponentHierarchy(null, null), targetImageFileSystemRootDir, layerTars, layerMapping);
        final ImageInfoParsed tarExtractionResults = tarParser.parseImageInfo(targetImageFileSystemRootDir);
        assertEquals("/var/lib/rpm", tarExtractionResults.getPkgMgr().getPackageManager().getDirectory());

        boolean varLibRpmNameFound = false;
        int numFilesFound = 0;
        final Collection<File> files = FileUtils.listFiles(workingDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        for (final File file : files) {
            numFilesFound++;
            if (file.getAbsolutePath().endsWith("var/lib/rpm/Name")) {
                System.out.println(file.getAbsolutePath());
                varLibRpmNameFound = true;
                final List<String> cmd = Arrays.asList("strings", file.getAbsolutePath());
                final String[] cmdOutput = Executor.executeCommand(cmd, 30000L);
                final String stringsOutput = Arrays.asList(cmdOutput).stream().collect(Collectors.joining("\n"));
                assertTrue(stringsOutput.contains("bacula-console"));
                assertTrue(stringsOutput.contains("bacula-client"));
                assertTrue(stringsOutput.contains("bacula-director"));
            }
        }
        assertTrue(varLibRpmNameFound);

        // MacOS file system does not preserve case which throws off the count
        System.out.printf("Extracted %d files\n", numFilesFound);
        assertTrue(numFilesFound > 18000);
        assertTrue(numFilesFound < 19000);
    }

    @Test
    public void testExtractDockerLayerTarSimple() throws WrongInspectorOsException, IOException {
        doLayerTest("simple");
    }

    private void doLayerTest(final String testFileDir) throws WrongInspectorOsException, IOException {
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
        tarParser.setManifestFactory(new ManifestFactory());

        final List<String> layerIds = new ArrayList<>();
        layerIds.add(LAYER_ID);
        final ManifestLayerMapping layerMapping = new ManifestLayerMapping(IMAGE_NAME, IMAGE_TAG, "test config filename", layerIds);

        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(IMAGE_NAME, IMAGE_TAG));
        tarParser.extractDockerLayers(new Gson(), new ComponentExtractorFactory(), OperatingSystemEnum.UBUNTU, new ImageComponentHierarchy(null, null), targetImageFileSystemRootDir, layerTars, layerMapping);
        assertEquals(tarExtractionDirectory.getAbsolutePath() + String.format("/imageFiles/%s", targetImageFileSystemRootDir.getName()), targetImageFileSystemRootDir.getAbsolutePath());

        final File dpkgStatusFile = new File(workingDirectory.getAbsolutePath() + String.format("/tarExtraction/imageFiles/%s/var/lib/dpkg/status", targetImageFileSystemRootDir.getName()));
        assertTrue(dpkgStatusFile.exists());

        assertEquals(DPKG_STATUS_FILE_SIZE, FileUtils.sizeOf(dpkgStatusFile));
    }

    @Test
    public void testCreateInitialImageComponentHierarchy() throws IntegrationException {
        File workingDir = new File("build/images/test/alpine");
        String tarFilename = "alpine.tar";

        final DockerTarParser tarParser = new DockerTarParser();
        tarParser.setManifestFactory(new ManifestFactory());
        ManifestLayerMapping mapping = tarParser.getLayerMapping(new GsonBuilder(), workingDir, tarFilename, "alpine", "latest");
        ImageComponentHierarchy h = tarParser.createInitialImageComponentHierarchy(workingDir, tarFilename, mapping);
        System.out.printf("Image config file contents: %s\n", h.getImageConfigFileContents());
        System.out.printf("Manifest file contents: %s\n", h.getManifestFileContents());
        assertTrue(h.getImageConfigFileContents().contains("architecture"));
        assertTrue(h.getManifestFileContents().contains("Config"));
    }
}
