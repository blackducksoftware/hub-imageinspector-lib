package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;

public class WhiteoutFileTest {
    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private static final String IMAGE_NAME = "blackducksoftware/centos_minus_vim_plus_bacula";
    private static final String IMAGE_TAG = "1.0";
    private static final String LAYER_ID = "layerId1";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testExtractDockerLayerTarWhiteoutOpaqueDir() throws WrongInspectorOsException, IOException {
        doLayerTest("whiteout");
    }

    private void doLayerTest(final String testFileDir) throws WrongInspectorOsException, IOException {
        final File workingDirectory = TestUtils.createTempDirectory();
        final File tarExtractionDirectory = new File(workingDirectory, ImageInspector.TAR_EXTRACTION_DIRECTORY);
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
        tarParser.setFileOperations(new FileOperations());
        tarParser.setImageConfigParser(new ImageConfigParser());
        tarParser.setLayerConfigParser(new LayerConfigParser());
        tarParser.setDockerLayerTarExtractor(new DockerLayerTarExtractor());
        tarParser.setPkgMgrExecutor(new PkgMgrExecutor());
        tarParser.setExecutor(new CmdExecutor());
        tarParser.setOs(new Os());

        final List<String> layerIds = new ArrayList<>();
        layerIds.add(LAYER_ID);
        final ManifestLayerMapping layerMapping = new ManifestLayerMapping(IMAGE_NAME, IMAGE_TAG, "test config filename", layerIds);

        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(IMAGE_NAME, IMAGE_TAG));
        tarParser.extractImageLayers(new GsonBuilder(), ImageInspectorOsEnum.UBUNTU, new ImageComponentHierarchy(null, null), targetImageFileSystemRootDir, layerTars, layerMapping, null);
        final File opaqueDir = new File(targetImageFileSystemRootDir, "opaque");
        assertFalse("Whited-out opaque dir was created", opaqueDir.exists());
    }

}
