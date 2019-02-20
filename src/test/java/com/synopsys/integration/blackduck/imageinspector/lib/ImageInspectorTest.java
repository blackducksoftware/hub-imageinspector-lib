package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.extraction.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extraction.ComponentExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

public class ImageInspectorTest {

    @Test
    public void test() throws IOException, IntegrationException {
        final DockerTarParser tarParser = Mockito.mock(DockerTarParser.class);
        final ComponentExtractorFactory componentExtractorFactory = Mockito.mock(ComponentExtractorFactory.class);
        final ImageInspector imageInspector = new ImageInspector(tarParser, componentExtractorFactory);

        final File workingDir = new File("src/test/resources/working");
        final File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        assertTrue(tarExtractionDirectory.getAbsolutePath().endsWith("src/test/resources/working/tarExtraction"));

        final File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        final List<File> layerTars = imageInspector.extractLayerTars(tarExtractionDirectory, dockerTarfile);
        Mockito.verify(tarParser).extractLayerTars(tarExtractionDirectory, dockerTarfile);

        final GsonBuilder gsonBuilder = new GsonBuilder();
        final String imageRepo = "alpine";
        final String imageTag = "latest";
        imageInspector.getLayerMapping(gsonBuilder, tarExtractionDirectory, dockerTarfile.getName(), imageRepo, imageTag);
        Mockito.verify(tarParser).getLayerMapping(gsonBuilder, tarExtractionDirectory, dockerTarfile.getName(), imageRepo, imageTag);

        final String imageConfigFileContents = "testConfig";
        final List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        final ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        imageInspector.createInitialImageComponentHierarchy(workingDir, tarExtractionDirectory, dockerTarfile.getName(), manifestLayerMapping);
        Mockito.verify(tarParser).createInitialImageComponentHierarchy(tarExtractionDirectory, dockerTarfile.getName(), manifestLayerMapping);

        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        final String manifestFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/alpine.tar/manifest.json"), StandardCharsets.UTF_8);
        final ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy( manifestFileContents, imageConfigFileContents);
            imageInspector.extractDockerLayers(ImageInspectorOsEnum.ALPINE, imageComponentHierarchy, targetImageFileSystemRootDir, layerTars, manifestLayerMapping);
        Mockito.verify(tarParser).extractDockerLayers(componentExtractorFactory, ImageInspectorOsEnum.ALPINE, imageComponentHierarchy, targetImageFileSystemRootDir, layerTars, manifestLayerMapping);

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
        final ImageInfoParsed imageInfoParsed = new ImageInfoParsed(targetImageFileSystemRootDir, imagePkgMgrDatabase, linuxDistroName, pkgMgr);
        final ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromGivenComponents(bdioGenerator, imageInfoParsed, imageComponentHierarchy, manifestLayerMapping, blackDuckProjectName, blackDuckProjectVersion,
            codeLocationPrefix, organizeComponentsByLayer, includeRemovedComponents);
        assertEquals(blackDuckProjectName, imageInfoDerived.getFinalProjectName());
        assertEquals(blackDuckProjectVersion, imageInfoDerived.getFinalProjectVersionName());
        assertEquals("testCodeLocationPrefix_alpine_latest_APK", imageInfoDerived.getCodeLocationName());
        assertEquals("APK", imageInfoDerived.getImageInfoParsed().getImagePkgMgrDatabase().getPackageManager().name());
        assertEquals(imageRepo, imageInfoDerived.getManifestLayerMapping().getImageName());
        assertEquals(layers.get(1), imageInfoDerived.getManifestLayerMapping().getLayers().get(1));
        assertEquals(String.format("%s/%s", blackDuckProjectName, blackDuckProjectVersion), imageInfoDerived.getBdioDocument().project.bdioExternalIdentifier.externalId);
    }
}
