package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.linux.TarOperations;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.Manifest;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.TargetImageFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;

public class DockerTarParserTest {
    private FileOperations fileOperations;
    private Os os;
    private PkgMgr pkgMgr;
    private ManifestFactory manifestFactory;
    private CmdExecutor cmdExecutor;
    private PkgMgrExecutor pkgMgrExecutor;
    private ImageConfigParser imageConfigParser;
    private LayerConfigParser layerConfigParser;
    private DockerLayerTarExtractor dockerLayerTarExtractor;
    private List<PkgMgr> pkgMgrs;
    private DockerTarParser tarParser;
    private TarOperations tarOperations;

    @BeforeEach
    public void setUpEach() {
        fileOperations = Mockito.mock(FileOperations.class);
        os = Mockito.mock(Os.class);
        pkgMgr = Mockito.mock(PkgMgr.class);
        Mockito.when(pkgMgr.isApplicable(Mockito.any(File.class))).thenReturn(Boolean.TRUE);
        final File mockApkDir = new File("src/test/resources/imageDir/alpine/lib/apk");
        Mockito.when(pkgMgr.getImagePackageManagerDirectory(Mockito.any(File.class))).thenReturn(mockApkDir);
        Mockito.when(pkgMgr.getType()).thenReturn(PackageManagerEnum.APK);

        manifestFactory = Mockito.mock(ManifestFactory.class);
        cmdExecutor = Mockito.mock(CmdExecutor.class);
        pkgMgrExecutor = Mockito.mock(PkgMgrExecutor.class);

        imageConfigParser = Mockito.mock(ImageConfigParser.class);
        layerConfigParser = Mockito.mock(LayerConfigParser.class);
        Mockito.when(layerConfigParser.parseCmd(Mockito.any(GsonBuilder.class), Mockito.anyString())).thenReturn(Arrays.asList("testLayerCmd", "testLayerCmdArg"));
        dockerLayerTarExtractor = Mockito.mock(DockerLayerTarExtractor.class);
        pkgMgrs = new ArrayList<>(1);
        pkgMgrs.add(pkgMgr);

        tarParser = new DockerTarParser();
        tarParser.setPkgMgrs(pkgMgrs);
        tarParser.setOs(os);
        tarParser.setManifestFactory(manifestFactory);
        tarParser.setExecutor(cmdExecutor);
        tarParser.setFileOperations(fileOperations);
        tarParser.setImageConfigParser(imageConfigParser);
        tarParser.setLayerConfigParser(layerConfigParser);
        tarParser.setPkgMgrExecutor(pkgMgrExecutor);
        tarParser.setDockerLayerTarExtractor(dockerLayerTarExtractor);

        tarOperations = new TarOperations();
        tarOperations.setFileOperations(fileOperations);
    }

    @Test
    public void testUnPackImageTar() throws IOException {

        final File dockerTar = new File("src/test/resources/mockDockerTar/alpine.tar");
        final File tarExtractionDirectory = new File("test/extraction");
        FileUtils.deleteDirectory(tarExtractionDirectory);
        tarExtractionDirectory.mkdir();
        // TODO Should test these separately?
        File extractionDir = tarOperations.extractTarToGivenBaseDir(tarExtractionDirectory, dockerTar);
        List<TypedArchiveFile> layerTars = tarParser.getLayerArchives(extractionDir);
        assertEquals(1, layerTars.size());
        assertEquals("layer.tar", layerTars.get(0).getFile().getName());
    }

    @Test
    public void testGetLayerMapping() throws IOException, IntegrationException {
        final String imageTarFilename = "alpine.tar";
        final String imageName = "alpine";
        final String imageTag = "latest";

        final GsonBuilder gsonBuilder = new GsonBuilder();
        final File tarExtractionDirectory = new File("test/extraction");
        FileUtils.deleteDirectory(tarExtractionDirectory);
        tarExtractionDirectory.mkdir();

        Manifest manifest = Mockito.mock(Manifest.class);
        Mockito.when(manifestFactory.createManifest(Mockito.any(File.class))).thenReturn(manifest);
        final String imageConfigFilename = "caf27325b298a6730837023a8a342699c8b7b388b8d878966b064a1320043019.json";
        final List<String> layerInternalIds = Arrays.asList("testLayer1", "testLayer2");
        final List<String> layerExternalIds = Arrays.asList("sha:Layer1", "sha:Layer2");
        ManifestLayerMapping layerMapping = new ManifestLayerMapping(imageName, imageTag, imageConfigFilename, layerInternalIds);
        Mockito.when(manifest.getLayerMapping(Mockito.anyString(), Mockito.anyString())).thenReturn(layerMapping);
        final String imageConfigFileTestDataPath = String.format("src/test/resources/mockDockerTarContents/%s", imageConfigFilename);
        final String imageConfigFileMockedPath = String.format("test/extraction/alpine.tar/%s", imageConfigFilename);
        final File imageConfigTestDataFile = new File(imageConfigFileTestDataPath);
        final File imageConfigMockedFile = new File(imageConfigFileMockedPath);
        final String imageConfigFileContents = FileUtils.readFileToString(imageConfigTestDataFile, StandardCharsets.UTF_8);
        Mockito.when(fileOperations
                         .readFileToString(imageConfigMockedFile)).thenReturn(imageConfigFileContents);
        Mockito.when(imageConfigParser.parseExternalLayerIds(gsonBuilder, imageConfigFileContents)).thenReturn(layerExternalIds);
        ManifestLayerMapping mapping = tarParser.getLayerMapping(gsonBuilder, tarExtractionDirectory, imageTarFilename, imageName, imageTag);
        assertEquals(imageName, mapping.getImageName());
        assertEquals(imageTag, mapping.getTagName());
        assertEquals(layerInternalIds.get(0), mapping.getLayerInternalIds().get(0));
        assertEquals(layerExternalIds.get(0), mapping.getLayerExternalId(0));
        assertEquals(layerInternalIds.get(1), mapping.getLayerInternalIds().get(1));
        assertEquals(layerExternalIds.get(1), mapping.getLayerExternalId(1));
    }

    @Test
    public void testCreateInitialImageComponentHierarchy() throws IntegrationException {

        final String imageTarFilename = "alpine.tar";
        final String imageName = "alpine";
        final String imageTag = "latest";
        final String imageConfigFileName = "caf27325b298a6730837023a8a342699c8b7b388b8d878966b064a1320043019.json";

        final File tarExtractionDirectory = new File("test/extraction");
        final File mockedImageTarContentsDir = new File("src/test/resources/mockDockerTarContents");
        final List<String> layerInternalIds = Arrays.asList("testLayer1", "testLayer2");
        final List<String> layerExternalIds = Arrays.asList("sha:Layer1", "sha:Layer2");

        final ManifestLayerMapping partialManifestLayerMapping = new ManifestLayerMapping(imageName, imageTag, imageConfigFileName, layerInternalIds);
        final ManifestLayerMapping fullManifestLayerMapping = new ManifestLayerMapping(partialManifestLayerMapping, layerExternalIds);

        final File manifestFile = new File(mockedImageTarContentsDir, "manifest.json");
        final File configFile = new File(mockedImageTarContentsDir, imageConfigFileName);
        final File[] filesInImageTar = { manifestFile, configFile };
        Mockito.when(fileOperations.listFilesInDir(new File(tarExtractionDirectory, imageTarFilename))).thenReturn(filesInImageTar);
        final ImageComponentHierarchy imageComponentHierarchy = tarParser.createInitialImageComponentHierarchy(tarExtractionDirectory, imageTarFilename, fullManifestLayerMapping);
        assertTrue(imageComponentHierarchy.getManifestFileContents().contains(String.format("%s:%s", imageName, imageTag)));
        assertTrue(imageComponentHierarchy.getImageConfigFileContents().contains("sha256:503e53e365f34399c4d58d8f4e23c161106cfbce4400e3d0a0357967bad69390"));
    }

    @Test
    public void testExtractImageLayersFull() throws IOException, IntegrationException, InterruptedException {
        final ImageComponentHierarchy imageComponentHierarchy = doExtractImageLayersTest(false);
        assertEquals("testCompName", imageComponentHierarchy.getFinalComponents().get(0).getName());
        assertEquals("Layer00_sha_Layer1", imageComponentHierarchy.getLayers().get(0).getLayerIndexedName());
        assertEquals("testCompName", imageComponentHierarchy.getLayers().get(0).getComponents().get(0).getName());
    }


    @Test
    public void testExtractImageLayersApp() throws IOException, IntegrationException, InterruptedException {
        final ImageComponentHierarchy imageComponentHierarchy = doExtractImageLayersTest(true);
        assertEquals(0, imageComponentHierarchy.getFinalComponents().size());
        assertEquals("Layer00_sha_Layer1", imageComponentHierarchy.getLayers().get(0).getLayerIndexedName());
        assertEquals("testCompName", imageComponentHierarchy.getLayers().get(0).getComponents().get(0).getName());
    }

    @Test
    public void testIgnoreUnreadableDistroFiles() {
        final DockerTarParser tarParserWithRealOsObject;
        tarParserWithRealOsObject = new DockerTarParser();
        tarParserWithRealOsObject.setPkgMgrs(pkgMgrs);
        tarParserWithRealOsObject.setOs(new Os());
        tarParserWithRealOsObject.setManifestFactory(manifestFactory);
        tarParserWithRealOsObject.setExecutor(cmdExecutor);
        tarParserWithRealOsObject.setFileOperations(fileOperations);
        tarParserWithRealOsObject.setImageConfigParser(imageConfigParser);
        tarParserWithRealOsObject.setLayerConfigParser(layerConfigParser);
        tarParserWithRealOsObject.setPkgMgrExecutor(pkgMgrExecutor);
        tarParserWithRealOsObject.setDockerLayerTarExtractor(dockerLayerTarExtractor);

        final File[] etcFiles = {
            new File("thisdirdoesnotexist/os-release"),
                                    new File("src/test/resources/osdetection/fedora/redhat-release")
        };
        File etcDir = new File("src/test/resources/osdetection/fedora");
        Mockito.when(fileOperations.listFilesInDir(etcDir)).thenReturn(etcFiles);
        Optional<String> distroFound = tarParserWithRealOsObject.extractLinuxDistroNameFromEtcDir(etcDir);
        assertEquals("fedora", distroFound.get());
    }

    private ImageComponentHierarchy doExtractImageLayersTest(final boolean excludePlatform) throws IOException, IntegrationException, InterruptedException {
        final String imageName = "alpine";
        final String imageTag = "latest";
        final String imageConfigFileName = "caf27325b298a6730837023a8a342699c8b7b388b8d878966b064a1320043019.json";

        final File mockedImageTarContentsDir = new File("src/test/resources/mockDockerTarContents");
        final List<String> layerInternalIds = Arrays.asList("03b951adf840798cb236a62db6705df7fb2f1e60e6f5fb93499ee8a566bd4114");
        final List<String> layerExternalIds = Arrays.asList("sha:Layer1");
        final String platformTopLayerId;
        if (excludePlatform) {
            platformTopLayerId = "sha:Layer1";
        } else {
            platformTopLayerId = null;
        }
        final ManifestLayerMapping partialManifestLayerMapping = new ManifestLayerMapping(imageName, imageTag, imageConfigFileName, layerInternalIds);
        final ManifestLayerMapping fullManifestLayerMapping = new ManifestLayerMapping(partialManifestLayerMapping, layerExternalIds);

        final PkgMgrFactory pkgMgrFactory = Mockito.mock(PkgMgrFactory.class);

        final File manifestFile = new File(mockedImageTarContentsDir, "manifest.json");
        final File configFile = new File(mockedImageTarContentsDir, imageConfigFileName);
        final String manifestFileContents = FileUtils.readFileToString(manifestFile, StandardCharsets.UTF_8);
        final String imageConfigFileContents = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
        final ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy(manifestFileContents, imageConfigFileContents);
        final File containerFileSystemRootDir = new File("test/containerFileSystemRoot");
        final TargetImageFileSystem targetImageFileSystem = new TargetImageFileSystem(containerFileSystemRootDir);
        final File dockerLayerTar = new File("src/test/resources/mockDockerTarContents/03b951adf840798cb236a62db6705df7fb2f1e60e6f5fb93499ee8a566bd4114/layer.tar");
        final List<TypedArchiveFile> layerTars = Arrays.asList(new TypedArchiveFile(ArchiveFileType.TAR, dockerLayerTar));

        Mockito.when(dockerLayerTarExtractor.extractLayerTarToDir(Mockito.any(FileOperations.class), Mockito.any(File.class), Mockito.any(File.class))).thenReturn(new ArrayList<>(0));

        final String[] apkOutput = { "comp1", "comp2"};
        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(CmdExecutor.class), Mockito.any(PkgMgr.class), Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(apkOutput);
        final List<ComponentDetails> comps = new ArrayList<>();
        final ComponentDetails comp = new ComponentDetails("testCompName", "testCompVersion", "testCompExternalId", "testCompArch", "testCompLinuxDistro");
        comps.add(comp);
        Mockito.when(pkgMgr.extractComponentsFromPkgMgrOutput(containerFileSystemRootDir, "alpine", apkOutput)).thenReturn(comps);
        final File imageEtcDir = new File("test/containerFileSystemRoot/etc");
        Mockito.when(fileOperations.isDirectory(imageEtcDir)).thenReturn(Boolean.TRUE);
        final File osReleaseFile = new File("test/containerFileSystemRoot/etc/os-release");
        final File[] etcDirFiles = { osReleaseFile };
        Mockito.when(fileOperations.listFilesInDir(imageEtcDir)).thenReturn(etcDirFiles);
        Mockito.when(os.getLinuxDistroNameFromEtcDir(imageEtcDir)).thenReturn(Optional.of("alpine"));
        ImageInfoParsed imageInfoParsed = tarParser.extractImageLayers(new GsonBuilder(), ImageInspectorOsEnum.ALPINE, null, imageComponentHierarchy,
        targetImageFileSystem, layerTars, fullManifestLayerMapping, platformTopLayerId);
        return imageComponentHierarchy;
    }
}
