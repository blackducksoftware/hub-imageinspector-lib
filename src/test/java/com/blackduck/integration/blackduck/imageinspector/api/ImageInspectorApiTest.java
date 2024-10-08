package com.blackduck.integration.blackduck.imageinspector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

//public class ImageInspectorApiTest {
//    // TODO split this test up; not sure it's worth getting it working
//    // TODO add test for invalid platformTop specified: should throw exception
//    @Disabled
//    @Test
//    public void test() throws IntegrationException, IOException, InterruptedException {
//        File workingDir = new File("test/working");
//        File targetDir = new File(workingDir, "target");
//        File tarExtractionDirectory = new File(workingDir, "extractionDir");
//        File containerFileSystemRootDir = new File(tarExtractionDirectory,
//            "imageFiles/image_testImageRepo_v_testTag");
//        ContainerFileSystem containerFileSystem = new ContainerFileSystem(containerFileSystemRootDir);
//        String tarFilename = "alpine_latest.tar";
//        File dockerTarfile = new File(targetDir, tarFilename);
//        File imageDir = new File(tarExtractionDirectory, tarFilename);
//        String dockerImageName = "testImageRepo";
//        String dockerTagName = "testTag";
//
//        String blackDuckProjectName = "testProjectName";
//        String blackDuckProjectVersion = "testProjectVersion";
//        String codeLocationPrefix = "testCodeLocationPrefix";
//        boolean organizeComponentsByLayer = true;
//        boolean includeRemovedComponents = true;
//        boolean cleanupWorkingDir = true;
//        String containerFileSystemOutputPath = "test/testContainerFileSystemOutputPath";
//        String currentLinuxDistro = "alpine";
//
//        BdioGenerator bdioGenerator = TestUtils.createBdioGenerator();
//        Gson gson = new Gson();
//
//        ImageInspector imageInspector = Mockito.mock(ImageInspector.class);
//        Mockito.when(imageInspector.getTarExtractionDirectory(Mockito.any(File.class)))
//            .thenReturn(tarExtractionDirectory);
//
//        DockerImageDirectory dockerImageDirectory = Mockito.mock(DockerImageDirectory.class);
//        List<TypedArchiveFile> layerTarFiles = new ArrayList<>();
//        Mockito.when(imageInspector.extractImageTar(Mockito.any(File.class), Mockito.any(File.class)))
//            .thenReturn(dockerImageDirectory);
//        Mockito.when(dockerImageDirectory.getLayerArchives())
//                .thenReturn(layerTarFiles);
//        List<String> layers = new ArrayList<>();
//        ManifestLayerMapping mapping = new ManifestLayerMapping(dockerImageName, dockerTagName, "testConfig", layers);
//        FullLayerMapping fullLayerMapping = new FullLayerMapping(mapping, new ArrayList<>(0));
//        Mockito.when(dockerImageDirectory
//                         .getLayerMapping(dockerImageName,
//                             dockerTagName)).thenReturn(fullLayerMapping);
//
//        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy();
//        List<ComponentDetails> components = new ArrayList<>();
//        ComponentDetails comp = new ComponentDetails("testCompName", "testCompVersion",
//            "testCompExternalId", "testCompArchitecture", "testLinuxDistroName");
//        components.add(comp);
//        LayerDetails layerDetails = new LayerDetails(0, "layer00", Arrays.asList("layerCmd", "layerCmdArg"), components);
//        imageComponentHierarchy.addLayer(layerDetails);
//        imageComponentHierarchy.setFinalComponents(components);
//
//        ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = new ContainerFileSystemWithPkgMgrDb(
//                containerFileSystem,
//            new ImagePkgMgrDatabase(new File("test/working/containerfilesystem/etc/apk"),
//                PackageManagerEnum.APK), "apline", new ApkPkgMgr(new FileOperations()));
//        // TODO this mock needs behavior, presumably
//        PackageGetter packageGetter = Mockito.mock(PackageGetter.class);
//
//        // TODO this should be a mock; make sure this class is tested in it's own test class
//        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
//        Mockito.when(imageInspector
//                         .extractDockerLayers(ImageInspectorOsEnum.ALPINE, null,
//                                 containerFileSystem,
//                             layerTarFiles, fullLayerMapping, null, componentHierarchyBuilder)).thenReturn(containerFileSystemWithPkgMgrDb);
//
//        ImageInfoDerived imageInfoDerived = new ImageInfoDerived(containerFileSystemWithPkgMgrDb, imageComponentHierarchy);
//        SimpleBdioDocument bdioDoc = new SimpleBdioDocument();
//        bdioDoc.setProject(new BdioProject());
//        bdioDoc.getProject().name = blackDuckProjectName;
//        bdioDoc.getProject().version = blackDuckProjectVersion;
//        imageInfoDerived.setBdioDocument(bdioDoc);
//        Mockito.when(imageInspector
//                         .generateBdioFromGivenComponents(bdioGenerator, containerFileSystemWithPkgMgrDb,
//                             fullLayerMapping, imageComponentHierarchy,
//                                 blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix,
//                             organizeComponentsByLayer, includeRemovedComponents, false)).thenReturn(imageInfoDerived);
//        Os os = Mockito.mock(Os.class);
//        Mockito.when(os.deriveOs("alpine")).thenReturn(ImageInspectorOsEnum.ALPINE);
//        ImageInspectorApi api = new ImageInspectorApi(imageInspector, os);
//        api.setGson(gson);
//        api.setBdioGenerator(bdioGenerator);
//        FileOperations fileOperations = Mockito.mock(FileOperations.class);
//        Mockito.when(fileOperations.createTempDirectory()).thenReturn(new File("test"));
//        api.setFileOperations(fileOperations);
//        // TODO these 2 mocks might need behavior
//        PkgMgrExecutor pkgMgrExecutor = Mockito.mock(PkgMgrExecutor.class);
//        api.setPkgMgrExecutor(pkgMgrExecutor);
//        CmdExecutor cmdExecutor = Mockito.mock(CmdExecutor.class);
//        api.setCmdExecutor(cmdExecutor);
//
////        PackageGetter packageGetter = new PackageGetter(pkgMgrExecutor, cmdExecutor);
////        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
//
//        ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
//                                                            .setDockerTarfilePath(dockerTarfile.getAbsolutePath())
//                                                            .setBlackDuckProjectName(blackDuckProjectName)
//                                                            .setBlackDuckProjectVersion(blackDuckProjectVersion)
//                                                            .setCodeLocationPrefix(codeLocationPrefix)
//                                                            .setGivenImageRepo(dockerImageName)
//                                                            .setGivenImageTag(dockerTagName)
//                                                            .setOrganizeComponentsByLayer(organizeComponentsByLayer)
//                                                            .setIncludeRemovedComponents(includeRemovedComponents)
//                                                            .setCleanupWorkingDir(cleanupWorkingDir)
//                                                            .setContainerFileSystemOutputPath(containerFileSystemOutputPath)
//                                                            .setCurrentLinuxDistro(currentLinuxDistro)
//                                                            .build();
//        SimpleBdioDocument result = api
//                                        .getBdio(componentHierarchyBuilder, imageInspectionRequest);
//        assertEquals(blackDuckProjectName, result.getProject().name);
//        assertEquals(blackDuckProjectVersion, result.getProject().version);
//    }
//}
