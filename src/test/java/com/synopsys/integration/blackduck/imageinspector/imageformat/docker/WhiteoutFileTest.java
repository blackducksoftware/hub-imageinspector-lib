package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.lib.FullLayerMapping;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.ContainerFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;

public class WhiteoutFileTest {
    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private static final String IMAGE_NAME = "blackducksoftware/centos_minus_vim_plus_bacula";
    private static final String IMAGE_TAG = "1.0";
    private static final String LAYER_ID = "layerId1";

    @BeforeAll
    public static void setUpBeforeAll() throws Exception {
    }

    @AfterAll
    public static void tearDownAfterAll() throws Exception {
    }

    @Test
    public void testExtractDockerLayerTarOpaqueDir() throws WrongInspectorOsException, IOException {
        final File targetImageFileSystemRootDir = doLayerTest("opaquedir");

        final File opaqueDir = new File(targetImageFileSystemRootDir, "opaque");
        assertTrue(opaqueDir.exists(), "Opaque dir was not created");

        final File opaqueDirAddedFile = new File(opaqueDir, "keep.txt");
        assertTrue(opaqueDirAddedFile.exists(), "Opaque dir added file was not created");
    }

    @Test
    public void testExtractDockerLayerTarPlnkDir() throws WrongInspectorOsException, IOException {
        final File targetImageFileSystemRootDir = doLayerTest("omitdir");

        final File omitDir = new File(targetImageFileSystemRootDir, "omit");
        assertFalse(omitDir.exists(), "Omit dir was created");
    }

    private File doLayerTest(final String testFileDir) throws WrongInspectorOsException, IOException {
        final File workingDirectory = TestUtils.createTempDirectory();
        final File tarExtractionDirectory = new File(workingDirectory, ImageInspector.TAR_EXTRACTION_DIRECTORY);
        final File layerDir = new File(tarExtractionDirectory, String.format("ubuntu_latest.tar/%s", LAYER_ID));
        layerDir.mkdirs();
        final Path layerDirPath = Paths.get(layerDir.getAbsolutePath());
        assertEquals("layerId1", layerDirPath.getFileName().toString());

        final File dockerTar = new File(layerDir, "layer.tar");
        Files.copy(new File(String.format("src/test/resources/%s/layer.tar", testFileDir)).toPath(), dockerTar.toPath(), REPLACE_EXISTING);
        final List<TypedArchiveFile> layerTars = new ArrayList<>();
        layerTars.add(new TypedArchiveFile(ArchiveFileType.TAR, dockerTar));

        final DockerTarParser tarParser = new DockerTarParser();
        tarParser.setManifestFactory(new DockerManifestFactory());
        tarParser.setFileOperations(new FileOperations());
        tarParser.setImageConfigParser(new DockerImageConfigParser());
        tarParser.setLayerConfigParser(new DockerLayerConfigParser(new GsonBuilder()));
        tarParser.setDockerLayerTarExtractor(new DockerLayerTarExtractor());
        tarParser.setPkgMgrExecutor(new PkgMgrExecutor());
        tarParser.setExecutor(new CmdExecutor());
        tarParser.setOs(new Os());

        final List<String> layerIds = new ArrayList<>();
        layerIds.add(LAYER_ID);
        final ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(IMAGE_NAME, IMAGE_TAG, "test config filename", layerIds);
        FullLayerMapping fullLayerMapping = new FullLayerMapping(manifestLayerMapping, new ArrayList<>());

        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(IMAGE_NAME, IMAGE_TAG));
        final ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir);

        tarParser.extractPkgMgrDb(ImageInspectorOsEnum.UBUNTU, null, containerFileSystem, layerTars, fullLayerMapping, null);

        return targetImageFileSystemRootDir;
    }

}
