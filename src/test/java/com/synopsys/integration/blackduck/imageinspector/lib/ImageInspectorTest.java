package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

public class ImageInspectorTest {
    private DockerTarParser tarParser;
    private ImageInspector imageInspector;

    @BeforeEach
    public void setUpEach() {
        tarParser = Mockito.mock(DockerTarParser.class);
        imageInspector = new ImageInspector(tarParser);
    }

    @Test
    public void testGetTarExtractionDirectory() {
        File workingDir = new File("src/test/resources/working");
        File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        assertTrue(tarExtractionDirectory.getAbsolutePath().endsWith("src/test/resources/working/tarExtraction"));
    }

    @Test
    public void testExtractLayerTars() throws IOException {
        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        imageInspector.extractLayerTars(tarExtractionDirectory, dockerTarfile);
        Mockito.verify(tarParser).unPackImageTar(tarExtractionDirectory, dockerTarfile);
    }

    @Test
    public void testGetLayerMapping() throws IOException, IntegrationException {
        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        GsonBuilder gsonBuilder = new GsonBuilder();
        String imageRepo = "alpine";
        String imageTag = "latest";
        imageInspector.getLayerMapping(gsonBuilder, tarExtractionDirectory, dockerTarfile.getName(), imageRepo, imageTag);
        Mockito.verify(tarParser).getLayerMapping(gsonBuilder, tarExtractionDirectory, dockerTarfile.getName(), imageRepo, imageTag);
    }

    @Test
    public void testCreateInitialImageComponentHierarchy() throws IOException, IntegrationException {
        File workingDir = new File("src/test/resources/working");
        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        String imageRepo = "alpine";
        String imageTag = "latest";
        List<File> layerTars = new ArrayList<>();
        File layerTar = new File(tarExtractionDirectory, String.format("%s/aaa/layer.tar", dockerTarfile.getName()));
        layerTars.add(layerTar);

        String imageConfigFileContents = "testConfig";
        List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        imageInspector.createInitialImageComponentHierarchy(tarExtractionDirectory, dockerTarfile.getName(), manifestLayerMapping);
        Mockito.verify(tarParser).createInitialImageComponentHierarchy(tarExtractionDirectory, dockerTarfile.getName(), manifestLayerMapping);
    }

    @Test
    public void testExtractDockerLayers() throws IOException, WrongInspectorOsException {
        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        String imageRepo = "alpine";
        String imageTag = "latest";
        String imageConfigFileContents = "testConfig";
        List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        List<File> layerTars = new ArrayList<>();
        File layerTar = new File(tarExtractionDirectory, String.format("%s/aaa/layer.tar", dockerTarfile.getName()));
        layerTars.add(layerTar);

        File targetImageFileSystemParentDir = new File(tarExtractionDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        TargetImageFileSystem targetImageFileSystem = new TargetImageFileSystem(targetImageFileSystemRootDir);
        String manifestFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/alpine.tar/manifest.json"), StandardCharsets.UTF_8);
        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy(manifestFileContents, imageConfigFileContents);
        GsonBuilder gsonBuilder = new GsonBuilder();
        imageInspector.extractDockerLayers(gsonBuilder, ImageInspectorOsEnum.ALPINE, null, imageComponentHierarchy, targetImageFileSystem, layerTars, manifestLayerMapping, null);
        Mockito.verify(tarParser).extractImageLayers(gsonBuilder, ImageInspectorOsEnum.ALPINE, null, imageComponentHierarchy, targetImageFileSystem, layerTars, manifestLayerMapping, null);
    }

    @Test
    public void testGenerateBdioFromGivenComponentsFull() throws IOException {
        boolean platformComponentsExcluded = false;
        ImageInfoDerived imageInfoDerived = doGeneratedBdioFromGivenComponentsTest(platformComponentsExcluded);
        assertEquals("testCodeLocationPrefix_alpine_latest_APK", imageInfoDerived.getCodeLocationName());
    }

    @Test
    public void testGenerateBdioFromGivenComponentsApp() throws IOException {
        boolean platformComponentsExcluded = true;
        ImageInfoDerived imageInfoDerived = doGeneratedBdioFromGivenComponentsTest(platformComponentsExcluded);
        assertEquals("testCodeLocationPrefix_alpine_latest_app_APK", imageInfoDerived.getCodeLocationName());
    }

    private ImageInfoDerived doGeneratedBdioFromGivenComponentsTest(boolean platformComponentsExcluded) throws IOException {
        File workingDir = new File("src/test/resources/working");
        File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        String imageRepo = "alpine";
        String imageTag = "latest";
        String imageConfigFileContents = "testConfig";
        List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        File targetImageFileSystemParentDir = new File(tarExtractionDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        TargetImageFileSystem targetImageFileSystem = new TargetImageFileSystem(targetImageFileSystemRootDir);
        String manifestFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/alpine.tar/manifest.json"), StandardCharsets.UTF_8);
        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy(manifestFileContents, imageConfigFileContents);
        BdioGenerator bdioGenerator = TestUtils.createBdioGenerator();
        String blackDuckProjectName = "testProjectName";
        String blackDuckProjectVersion = "testProjectVersion";
        String codeLocationPrefix = "testCodeLocationPrefix";
        boolean organizeComponentsByLayer = true;
        boolean includeRemovedComponents = true;
        File extractedPackageManagerDirectory = new File("src/test/resources/imageDir/alpine/lib/apk");
        ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(extractedPackageManagerDirectory, PackageManagerEnum.APK);
        String linuxDistroName = "alpine";
        PkgMgr pkgMgr = new ApkPkgMgr(new FileOperations());
        ImageInfoParsed imageInfoParsed = new ImageInfoParsed(targetImageFileSystem, imagePkgMgrDatabase, linuxDistroName, pkgMgr);
        ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromGivenComponents(bdioGenerator, imageInfoParsed, imageComponentHierarchy, manifestLayerMapping, blackDuckProjectName, blackDuckProjectVersion,
            codeLocationPrefix, organizeComponentsByLayer, includeRemovedComponents, platformComponentsExcluded);
        assertEquals(blackDuckProjectName, imageInfoDerived.getFinalProjectName());
        assertEquals(blackDuckProjectVersion, imageInfoDerived.getFinalProjectVersionName());
        assertEquals("APK", imageInfoDerived.getImageInfoParsed().getImagePkgMgrDatabase().getPackageManager().name());
        assertEquals(imageRepo, imageInfoDerived.getManifestLayerMapping().getImageName());
        assertEquals(layers.get(1), imageInfoDerived.getManifestLayerMapping().getLayerInternalIds().get(1));
        assertEquals(String.format("%s/%s", blackDuckProjectName, blackDuckProjectVersion), imageInfoDerived.getBdioDocument().getProject().bdioExternalIdentifier.externalId);
        return imageInfoDerived;
    }

}
