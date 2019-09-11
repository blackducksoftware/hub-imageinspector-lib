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
        final File workingDir = new File("src/test/resources/working");
        final File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        assertTrue(tarExtractionDirectory.getAbsolutePath().endsWith("src/test/resources/working/tarExtraction"));
    }

    @Test
    public void testExtractLayerTars() throws IOException {
        final File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        final File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        imageInspector.extractLayerTars(tarExtractionDirectory, dockerTarfile);
        Mockito.verify(tarParser).unPackImageTar(tarExtractionDirectory, dockerTarfile);
    }
    
    @Test
    public void testGetLayerMapping() throws IOException, IntegrationException {
        final File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        final File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        final GsonBuilder gsonBuilder = new GsonBuilder();
        final String imageRepo = "alpine";
        final String imageTag = "latest";
        imageInspector.getLayerMapping(gsonBuilder, tarExtractionDirectory, dockerTarfile.getName(), imageRepo, imageTag);
        Mockito.verify(tarParser).getLayerMapping(gsonBuilder, tarExtractionDirectory, dockerTarfile.getName(), imageRepo, imageTag);
    }

    @Test
    public void testCreateInitialImageComponentHierarchy() throws IOException, IntegrationException {

        final File workingDir = new File("src/test/resources/working");
        final File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        final File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        final String imageRepo = "alpine";
        final String imageTag = "latest";
        final List<File> layerTars = new ArrayList<>();
        final File layerTar = new File(tarExtractionDirectory, String.format("%s/aaa/layer.tar", dockerTarfile.getName()));
        layerTars.add(layerTar);

        final String imageConfigFileContents = "testConfig";
        final List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        final ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        imageInspector.createInitialImageComponentHierarchy(workingDir, tarExtractionDirectory, dockerTarfile.getName(), manifestLayerMapping);
        Mockito.verify(tarParser).createInitialImageComponentHierarchy(tarExtractionDirectory, dockerTarfile.getName(), manifestLayerMapping);
    }

    @Test
    public void testExtractDockerLayers() throws IOException, WrongInspectorOsException {
        final File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        final File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        final String imageRepo = "alpine";
        final String imageTag = "latest";
        final String imageConfigFileContents = "testConfig";
        final List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        final ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        final List<File> layerTars = new ArrayList<>();
        final File layerTar = new File(tarExtractionDirectory, String.format("%s/aaa/layer.tar", dockerTarfile.getName()));
        layerTars.add(layerTar);

        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        final TargetImageFileSystem targetImageFileSystem = new TargetImageFileSystem(targetImageFileSystemRootDir);
        final String manifestFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/alpine.tar/manifest.json"), StandardCharsets.UTF_8);
        final ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy( manifestFileContents, imageConfigFileContents);
        final GsonBuilder gsonBuilder = new GsonBuilder();
        imageInspector.extractDockerLayers(gsonBuilder, ImageInspectorOsEnum.ALPINE, imageComponentHierarchy, targetImageFileSystem, layerTars, manifestLayerMapping, null);
        Mockito.verify(tarParser).extractImageLayers(gsonBuilder, ImageInspectorOsEnum.ALPINE, imageComponentHierarchy, targetImageFileSystem, layerTars, manifestLayerMapping, null);
    }

    @Test
    public void testGenerateBdioFromGivenComponentsFull() throws IOException, IntegrationException {
        final boolean platformComponentsExcluded=false;
        final ImageInfoDerived imageInfoDerived = doGeneratedBdioFromGivenComponentsTest(platformComponentsExcluded);
        assertEquals("testCodeLocationPrefix_alpine_latest_APK", imageInfoDerived.getCodeLocationName());
    }

    @Test
    public void testGenerateBdioFromGivenComponentsApp() throws IOException, IntegrationException {
        final boolean platformComponentsExcluded=true;
        final ImageInfoDerived imageInfoDerived = doGeneratedBdioFromGivenComponentsTest(platformComponentsExcluded);
        assertEquals("testCodeLocationPrefix_alpine_latest_app_APK", imageInfoDerived.getCodeLocationName());
    }

    private ImageInfoDerived doGeneratedBdioFromGivenComponentsTest(final boolean platformComponentsExcluded) throws IOException {
        final File workingDir = new File("src/test/resources/working");
        final File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        final String imageRepo = "alpine";
        final String imageTag = "latest";
        final String imageConfigFileContents = "testConfig";
        final List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        final ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        final TargetImageFileSystem targetImageFileSystem = new TargetImageFileSystem(targetImageFileSystemRootDir);
        final String manifestFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/alpine.tar/manifest.json"), StandardCharsets.UTF_8);
        final ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy( manifestFileContents, imageConfigFileContents);
        final BdioGenerator bdioGenerator = new BdioGenerator();
        final String blackDuckProjectName = "testProjectName";
        final String blackDuckProjectVersion = "testProjectVersion";
        final String codeLocationPrefix = "testCodeLocationPrefix";
        final boolean organizeComponentsByLayer = true;
        final boolean includeRemovedComponents = true;
        final File extractedPackageManagerDirectory = new File("src/test/resources/imageDir/alpine/lib/apk");
        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(extractedPackageManagerDirectory, PackageManagerEnum.APK);
        final String linuxDistroName = "alpine";
        final PkgMgr pkgMgr = new ApkPkgMgr(new FileOperations());
        final ImageInfoParsed imageInfoParsed = new ImageInfoParsed(targetImageFileSystem, imagePkgMgrDatabase, linuxDistroName, pkgMgr);
        final ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromGivenComponents(bdioGenerator, imageInfoParsed, imageComponentHierarchy, manifestLayerMapping, blackDuckProjectName, blackDuckProjectVersion,
            codeLocationPrefix, organizeComponentsByLayer, includeRemovedComponents, platformComponentsExcluded);
        assertEquals(blackDuckProjectName, imageInfoDerived.getFinalProjectName());
        assertEquals(blackDuckProjectVersion, imageInfoDerived.getFinalProjectVersionName());
        assertEquals("APK", imageInfoDerived.getImageInfoParsed().getImagePkgMgrDatabase().getPackageManager().name());
        assertEquals(imageRepo, imageInfoDerived.getManifestLayerMapping().getImageName());
        assertEquals(layers.get(1), imageInfoDerived.getManifestLayerMapping().getLayerInternalIds().get(1));
        assertEquals(String.format("%s/%s", blackDuckProjectName, blackDuckProjectVersion), imageInfoDerived.getBdioDocument().project.bdioExternalIdentifier.externalId);
        return imageInfoDerived;
    }
}
