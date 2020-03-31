package com.synopsys.integration.blackduck.imageinspector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.bdio.model.BdioProject;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMappingFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.TargetImageFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

public class ImageInspectorApiTest {
  // TODO split this test up
  // TODO add test for invalid platformTop specified: should throw exception

  @Test
  public void test() throws IntegrationException, IOException, InterruptedException {
    final File workingDir = new File("test/working");
    final File targetDir = new File(workingDir, "target");
    final File tarExtractionDirectory = new File(workingDir, "extractionDir");
    final File containerFileSystemRootDir = new File(tarExtractionDirectory,
        "imageFiles/image_testImageRepo_v_testTag");
    final TargetImageFileSystem targetImageFileSystem = new TargetImageFileSystem(containerFileSystemRootDir);
    final String tarFilename = "alpine_latest.tar";
    final File dockerTarfile = new File(targetDir, tarFilename);
    final String dockerImageName = "testImageRepo";
    final String dockerTagName = "testTag";

    final String blackDuckProjectName = "testProjectName";
    final String blackDuckProjectVersion = "testProjectVersion";
    final String codeLocationPrefix = "testCodeLocationPrefix";
    final boolean organizeComponentsByLayer = true;
    final boolean includeRemovedComponents = true;
    final boolean cleanupWorkingDir = true;
    final String containerFileSystemOutputPath = "test/testContainerFileSystemOutputPath";
    final String currentLinuxDistro = "alpine";

    final BdioGenerator bdioGenerator = new BdioGenerator();
    final GsonBuilder gsonBuilder = new GsonBuilder();

    final ImageInspector imageInspector = Mockito.mock(ImageInspector.class);
    Mockito.when(imageInspector.getTarExtractionDirectory(Mockito.any(File.class)))
        .thenReturn(tarExtractionDirectory);

    List<File> layerTarFiles = new ArrayList<>();
    Mockito.when(imageInspector.extractLayerTars(Mockito.any(File.class), Mockito.any(File.class)))
        .thenReturn(layerTarFiles);
    final List<String> layers = new ArrayList<>();
    ManifestLayerMapping mapping = (new ManifestLayerMappingFactory())
        .createManifestLayerMapping(dockerImageName, dockerTagName, "testConfig", layers);
    Mockito.when(imageInspector
        .getLayerMapping(gsonBuilder, tarExtractionDirectory, tarFilename, dockerImageName,
            dockerTagName)).thenReturn(mapping);

    ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy(
        "testManifestFileContents", "testImageConfigFileContents");
    final List<ComponentDetails> components = new ArrayList<>();
    final ComponentDetails comp = new ComponentDetails("testCompName", "testCompVersion",
        "testCompExternalId", "testCompArchitecture", "testLinuxDistroName");
    components.add(comp);
    final LayerDetails layerDetails = new LayerDetails(0, "layer00", "layerMetaData", Arrays.asList("layerCmd", "layerCmdArg"), components);
    imageComponentHierarchy.addLayer(layerDetails);
    imageComponentHierarchy.setFinalComponents(components);
    Mockito.when(imageInspector
        .createInitialImageComponentHierarchy(tarExtractionDirectory, tarFilename,
            mapping)).thenReturn(imageComponentHierarchy);

    final ImageInfoParsed imageInfoParsed = new ImageInfoParsed(
        targetImageFileSystem,
        new ImagePkgMgrDatabase(new File("test/working/containerfilesystem/etc/apk"),
            PackageManagerEnum.APK), "apline", new ApkPkgMgr(new FileOperations()));
    Mockito.when(imageInspector
        .extractDockerLayers(gsonBuilder, ImageInspectorOsEnum.ALPINE, null, imageComponentHierarchy,
            targetImageFileSystem,
            layerTarFiles, mapping, null)).thenReturn(imageInfoParsed);

    final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(imageInfoParsed);
    SimpleBdioDocument bdioDoc = new SimpleBdioDocument();
    bdioDoc.setProject(new BdioProject());
    bdioDoc.getProject().name = blackDuckProjectName;
    bdioDoc.getProject().version = blackDuckProjectVersion;
    imageInfoDerived.setBdioDocument(bdioDoc);
    Mockito.when(imageInspector
        .generateBdioFromGivenComponents(bdioGenerator, imageInfoParsed, imageComponentHierarchy,
            mapping, blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix,
            organizeComponentsByLayer, includeRemovedComponents, false)).thenReturn(imageInfoDerived);
    final Os os = Mockito.mock(Os.class);
    Mockito.when(os.deriveOs("alpine")).thenReturn(ImageInspectorOsEnum.ALPINE);
    final ImageInspectorApi api = new ImageInspectorApi(imageInspector, os);
    api.setGsonBuilder(gsonBuilder);
    api.setBdioGenerator(bdioGenerator);
    final FileOperations fileOperations = Mockito.mock(FileOperations.class);
    Mockito.when(fileOperations.createTempDirectory()).thenReturn(new File("test"));
    api.setFileOperations(fileOperations);

    final ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
        .setDockerTarfilePath(dockerTarfile.getAbsolutePath())
        .setBlackDuckProjectName(blackDuckProjectName)
        .setBlackDuckProjectVersion(blackDuckProjectVersion)
        .setCodeLocationPrefix(codeLocationPrefix)
        .setGivenImageRepo(dockerImageName)
        .setGivenImageTag(dockerTagName)
        .setOrganizeComponentsByLayer(organizeComponentsByLayer)
        .setIncludeRemovedComponents(includeRemovedComponents)
        .setCleanupWorkingDir(cleanupWorkingDir)
        .setContainerFileSystemOutputPath(containerFileSystemOutputPath)
        .setCurrentLinuxDistro(currentLinuxDistro)
        .build();
    final SimpleBdioDocument result = api
        .getBdio(imageInspectionRequest);
    assertEquals(blackDuckProjectName, result.getProject().name);
    assertEquals(blackDuckProjectVersion, result.getProject().version);
  }

}
