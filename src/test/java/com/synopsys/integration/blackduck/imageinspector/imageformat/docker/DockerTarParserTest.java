package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO rethink this whole class

//public class DockerTarParserTest {
//    private FileOperations fileOperations;
//    private Os os;
//    private PkgMgr pkgMgr;
//    private DockerManifestFactory dockerManifestFactory;
//    private CmdExecutor cmdExecutor;
//    private PkgMgrExecutor pkgMgrExecutor;
//    private DockerImageConfigParser dockerImageConfigParser;
//    private DockerLayerConfigParser dockerLayerConfigParser;
//    private ImageLayerArchiveExtractor imageLayerArchiveExtractor;
//    private List<PkgMgr> pkgMgrs;
//    private ContainerFileSystemAnalyzer containerFileSystemAnalyzer;
//    private TarOperations tarOperations;
//
//    @BeforeEach
//    public void setUpEach() {
//        fileOperations = Mockito.mock(FileOperations.class);
//        os = Mockito.mock(Os.class);
//        pkgMgr = Mockito.mock(PkgMgr.class);
//        Mockito.when(pkgMgr.isApplicable(Mockito.any(File.class))).thenReturn(Boolean.TRUE);
//        final File mockApkDir = new File("src/test/resources/imageDir/alpine/lib/apk");
//        Mockito.when(pkgMgr.getImagePackageManagerDirectory(Mockito.any(File.class))).thenReturn(mockApkDir);
//        Mockito.when(pkgMgr.getType()).thenReturn(PackageManagerEnum.APK);
//
//        dockerManifestFactory = Mockito.mock(DockerManifestFactory.class);
//        cmdExecutor = Mockito.mock(CmdExecutor.class);
//        pkgMgrExecutor = Mockito.mock(PkgMgrExecutor.class);
//
//        dockerImageConfigParser = Mockito.mock(DockerImageConfigParser.class);
//        dockerLayerConfigParser = Mockito.mock(DockerLayerConfigParser.class);
//        Mockito.when(dockerLayerConfigParser.parseCmd(Mockito.anyString())).thenReturn(Arrays.asList("testLayerCmd", "testLayerCmdArg"));
//        imageLayerArchiveExtractor = Mockito.mock(ImageLayerArchiveExtractor.class);
//        pkgMgrs = new ArrayList<>(1);
//        pkgMgrs.add(pkgMgr);
//
//        tarParser = new DockerTarParser();
//
//        tarOperations = new TarOperations();
//        tarOperations.setFileOperations(fileOperations);
//    }
//
//    @Test
//    public void testUnPackImageTar() throws IOException {
//
//        final File dockerTar = new File("src/test/resources/mockDockerTar/alpine.tar");
//        final File tarExtractionDirectory = new File("test/extraction");
//        FileUtils.deleteDirectory(tarExtractionDirectory);
//        tarExtractionDirectory.mkdir();
//        File imageDir = new File(tarExtractionDirectory, dockerTar.getName());
//        // TODO Should test these separately?
//        File extractionDir = tarOperations.extractTarToGivenDir(imageDir, dockerTar);
//        DockerImageDirectory dockerImageDirectory = new DockerImageDirectory(new GsonBuilder(), new FileOperations(), new DockerImageConfigParser(),
//                new DockerManifestFactory(), extractionDir);
//        List<TypedArchiveFile> layerTars = dockerImageDirectory.getLayerArchives();
//        assertEquals(1, layerTars.size());
//        assertEquals("layer.tar", layerTars.get(0).getFile().getName());
//    }
//
//    // TODO some of these tests, like this one, are testing methods that have moved to DockerImageReader
//    @Test
//    public void testGetLayerMapping() throws IOException, IntegrationException {
//        final String imageTarFilename = "alpine.tar";
//        final String imageName = "alpine";
//        final String imageTag = "latest";
//
//        final GsonBuilder gsonBuilder = new GsonBuilder();
//        final File tarExtractionDirectory = new File("test/extraction");
//        FileUtils.deleteDirectory(tarExtractionDirectory);
//        tarExtractionDirectory.mkdir();
//
//        DockerManifest manifest = Mockito.mock(DockerManifest.class);
//        Mockito.when(dockerManifestFactory.createManifest(Mockito.any(File.class))).thenReturn(manifest);
//        final String imageConfigFilename = "caf27325b298a6730837023a8a342699c8b7b388b8d878966b064a1320043019.json";
//        final List<String> layerInternalIds = Arrays.asList("testLayer1", "testLayer2");
//        final List<String> layerExternalIds = Arrays.asList("sha:Layer1", "sha:Layer2");
//        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageName, imageTag, imageConfigFilename, layerInternalIds);
//        Mockito.when(manifest.getLayerMapping(Mockito.anyString(), Mockito.anyString())).thenReturn(manifestLayerMapping);
//        final String imageConfigFileTestDataPath = String.format("src/test/resources/mockDockerTarContents/%s", imageConfigFilename);
//        final String imageConfigFileMockedPath = String.format("test/extraction/alpine.tar/%s", imageConfigFilename);
//        final File imageConfigTestDataFile = new File(imageConfigFileTestDataPath);
//        final File imageConfigMockedFile = new File(imageConfigFileMockedPath);
//        final String imageConfigFileContents = FileUtils.readFileToString(imageConfigTestDataFile, StandardCharsets.UTF_8);
//        Mockito.when(fileOperations
//                         .readFileToString(imageConfigMockedFile)).thenReturn(imageConfigFileContents);
//        Mockito.when(dockerImageConfigParser.parseExternalLayerIds(gsonBuilder, imageConfigFileContents)).thenReturn(layerExternalIds);
//        File imageDir = new File(tarExtractionDirectory, imageTarFilename);
//        DockerImageDirectory dockerImageDirectory = new DockerImageDirectory(gsonBuilder, fileOperations, dockerImageConfigParser, dockerManifestFactory, imageDir);
//        FullLayerMapping mapping = dockerImageDirectory.getLayerMapping(imageName, imageTag);
//        assertEquals(imageName, mapping.getManifestLayerMapping().getImageName());
//        assertEquals(imageTag, mapping.getManifestLayerMapping().getTagName());
//        assertEquals(layerInternalIds.get(0), mapping.getManifestLayerMapping().getLayerInternalIds().get(0));
//        assertEquals(layerExternalIds.get(0), mapping.getLayerExternalId(0));
//        assertEquals(layerInternalIds.get(1), mapping.getManifestLayerMapping().getLayerInternalIds().get(1));
//        assertEquals(layerExternalIds.get(1), mapping.getLayerExternalId(1));
//    }
//
////    @Test
////    public void testExtractImageLayersFull() throws IOException, IntegrationException, InterruptedException {
////        final ImageComponentHierarchy imageComponentHierarchy = doExtractImageLayersTest(false);
////        assertEquals("testCompName", imageComponentHierarchy.getFinalComponents().get(0).getName());
////        assertEquals("Layer00_sha_Layer1", imageComponentHierarchy.getLayers().get(0).getLayerIndexedName());
////        assertEquals("testCompName", imageComponentHierarchy.getLayers().get(0).getComponents().get(0).getName());
////    }
////
////
////    @Test
////    public void testExtractImageLayersApp() throws IOException, IntegrationException, InterruptedException {
////        final ImageComponentHierarchy imageComponentHierarchy = doExtractImageLayersTest(true);
////        assertEquals(0, imageComponentHierarchy.getFinalComponents().size());
////        assertEquals("Layer00_sha_Layer1", imageComponentHierarchy.getLayers().get(0).getLayerIndexedName());
////        assertEquals("testCompName", imageComponentHierarchy.getLayers().get(0).getComponents().get(0).getName());
////    }
//
//    @Test
//    public void testIgnoreUnreadableDistroFiles() {
//        final DockerTarParser tarParserWithRealOsObject;
//        tarParserWithRealOsObject = new DockerTarParser();
//
//        final File[] etcFiles = {
//            new File("thisdirdoesnotexist/os-release"),
//                                    new File("src/test/resources/osdetection/fedora/redhat-release")
//        };
//        File etcDir = new File("src/test/resources/osdetection/fedora");
//        Mockito.when(fileOperations.listFilesInDir(etcDir)).thenReturn(etcFiles);
//        // TODO this should be tested elsewhere
////        Optional<String> distroFound = tarParserWithRealOsObject.extractLinuxDistroNameFromEtcDir(etcDir);
////        assertEquals("fedora", distroFound.get());
//    }
//
//    // TODO Need tests for the new code: ImageInspector.extractDockerLayers()
////    private ImageComponentHierarchy doExtractImageLayersTest(final boolean excludePlatform) throws IOException, IntegrationException, InterruptedException {
////        final String imageName = "alpine";
////        final String imageTag = "latest";
////        final String imageConfigFileName = "caf27325b298a6730837023a8a342699c8b7b388b8d878966b064a1320043019.json";
////
////        final File mockedImageTarContentsDir = new File("src/test/resources/mockDockerTarContents");
////        final List<String> layerInternalIds = Arrays.asList("03b951adf840798cb236a62db6705df7fb2f1e60e6f5fb93499ee8a566bd4114");
////        final List<String> layerExternalIds = Arrays.asList("sha:Layer1");
////        final String platformTopLayerId;
////        if (excludePlatform) {
////            platformTopLayerId = "sha:Layer1";
////        } else {
////            platformTopLayerId = null;
////        }
////        final ManifestLayerMapping partialManifestLayerMapping = new ManifestLayerMapping(imageName, imageTag, imageConfigFileName, layerInternalIds);
////        final FullLayerMapping fullManifestLayerMapping = new FullLayerMapping(partialManifestLayerMapping, layerExternalIds);
////        final ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy();
////        final File containerFileSystemRootDir = new File("test/containerFileSystemRoot");
////        final ContainerFileSystem containerFileSystem = new ContainerFileSystem(containerFileSystemRootDir);
////        final File dockerLayerTar = new File("src/test/resources/mockDockerTarContents/03b951adf840798cb236a62db6705df7fb2f1e60e6f5fb93499ee8a566bd4114/layer.tar");
////        final List<TypedArchiveFile> layerTars = Arrays.asList(new TypedArchiveFile(ArchiveFileType.TAR, dockerLayerTar));
////
////        Mockito.when(dockerLayerTarExtractor.extractLayerTarToDir(Mockito.any(FileOperations.class), Mockito.any(File.class), Mockito.any(File.class))).thenReturn(new ArrayList<>(0));
////
////        final String[] apkOutput = { "comp1", "comp2"};
////        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(CmdExecutor.class), Mockito.any(PkgMgr.class), Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(apkOutput);
////        final List<ComponentDetails> comps = new ArrayList<>();
////        final ComponentDetails comp = new ComponentDetails("testCompName", "testCompVersion", "testCompExternalId", "testCompArch", "testCompLinuxDistro");
////        comps.add(comp);
////        Mockito.when(pkgMgr.extractComponentsFromPkgMgrOutput(containerFileSystemRootDir, "alpine", apkOutput)).thenReturn(comps);
////        final File imageEtcDir = new File("test/containerFileSystemRoot/etc");
////        Mockito.when(fileOperations.isDirectory(imageEtcDir)).thenReturn(Boolean.TRUE);
////        final File osReleaseFile = new File("test/containerFileSystemRoot/etc/os-release");
////        final File[] etcDirFiles = { osReleaseFile };
////        Mockito.when(fileOperations.listFilesInDir(imageEtcDir)).thenReturn(etcDirFiles);
////        Mockito.when(os.getLinuxDistroNameFromEtcDir(imageEtcDir)).thenReturn(Optional.of("alpine"));
////        ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = tarParser.extractPkgMgrDb(ImageInspectorOsEnum.ALPINE, null,
////                containerFileSystem, layerTars, fullManifestLayerMapping, platformTopLayerId);
////        return containerFileSystemWithPkgMgrDb.getImageComponentHierarchy();
////    }
//}
