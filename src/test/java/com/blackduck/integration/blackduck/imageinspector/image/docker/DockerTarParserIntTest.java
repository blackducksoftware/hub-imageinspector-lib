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
package com.blackduck.integration.blackduck.imageinspector.image.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO make sure this stuff is tested somewhere
//@Tag("integration")
//public class DockerTarParserIntTest {
//    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
//    private final static int DPKG_STATUS_FILE_SIZE = 98016;
//
//    private static final String IMAGE_NAME = "blackducksoftware/centos_minus_vim_plus_bacula";
//
//    private static final String IMAGE_TAG = "1.0";
//
//    private static final String LAYER_ID = "layerId1";
//    private static PkgMgr apkPkgMgr;
//    private static PkgMgr dpkgPkgMgr;
//    private static PkgMgr rpmPkgMgr;
//    private static List<PkgMgr> pkgMgrs;
//
//    @BeforeAll
//    public static void setup() {
//        FileOperations fileOperations = new FileOperations();
//        pkgMgrs = new ArrayList<>(3);
//        apkPkgMgr = new ApkPkgMgr(fileOperations);
//        dpkgPkgMgr = new DpkgPkgMgr(fileOperations);
//        rpmPkgMgr = new RpmPkgMgr(new Gson(), fileOperations);
//
//        pkgMgrs.add(apkPkgMgr);
//        pkgMgrs.add(dpkgPkgMgr);
//        pkgMgrs.add(rpmPkgMgr);
//    }
//    @Test
//    public void testParseImageInfoApk() throws PkgMgrDataNotFoundException {
//        testParseImageInfo("alpine", PackageManagerEnum.APK, "apk");
//    }
//
//    @Test
//    public void testParseImageInfoDpkg() throws PkgMgrDataNotFoundException {
//        testParseImageInfo("ubuntu", PackageManagerEnum.DPKG, "dpkg");
//    }
//
//    @Test
//    public void testParseImageInfoRpm() throws PkgMgrDataNotFoundException {
//        testParseImageInfo("centos", PackageManagerEnum.RPM, "rpm");
//    }
//
//
////    @Test
////    public void testExtractFullImage() throws IntegrationException, IOException {
////        final File dockerTar = new File("build/images/test/centos_minus_vim_plus_bacula.tar");
////        final File workingDirectory = TestUtils.createTempDirectory();
////        final File tarExtractionDirectory = new File(workingDirectory, ImageInspector.TAR_EXTRACTION_DIRECTORY);
////        File imageDir = new File(tarExtractionDirectory, dockerTar.getName());
////        System.out.println("workingDirectory: ${workingDirectory.getAbsolutePath()}");
////
////        // Mock the PkgMgrExecutor so it doesn't try to overwrite this machine's pkg mgr db
////        // (it runs the pkg mgr after unpacking each layer
////        PkgMgrExecutor pkgMgrExecutor = Mockito.mock(PkgMgrExecutor.class);
////        String[] pkgMgrOutput = {
////                "{ epoch: \"(none)\", name: \"centos-release\", version: \"7-3.1611.el7.centos\", arch: \"x86_64\" }",
////                "{ epoch: \"(none)\", name: \"filesystem\", version: \"3.2-21.el7\", arch: \"x86_64\" }",
////                "{ epoch: \"(none)\", name: \"basesystem\", version: \"10.0-7.el7.centos\", arch: \"noarch\" }"
////        };
////        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(CmdExecutor.class), Mockito.any(PkgMgr.class), Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(pkgMgrOutput);
////
////        final DockerTarParser tarParser = new DockerTarParser();
////        tarParser.setManifestFactory(new DockerManifestFactory());
////        tarParser.setOs(new Os());
////        tarParser.setFileOperations(new FileOperations());
////        tarParser.setPkgMgrs(pkgMgrs);
////        tarParser.setPkgMgrExecutor(pkgMgrExecutor);
////        tarParser.setDockerLayerTarExtractor(new DockerLayerTarExtractor());
////        tarParser.setImageConfigParser(new DockerImageConfigParser());
////        tarParser.setLayerConfigParser(new DockerLayerConfigParser(new Gson()));
////
////        TarOperations tarOperations = new TarOperations();
////        tarOperations.setFileOperations(new FileOperations());
////
////        File extractionDir = tarOperations.extractTarToGivenDir(imageDir, dockerTar);
////        DockerImageDirectory dockerImageDirectory = new DockerImageDirectory(new Gson(), new FileOperations(),
////                new DockerImageConfigParser(), new DockerManifestFactory(), extractionDir);
////        final List<TypedArchiveFile> layerTars = dockerImageDirectory.getLayerArchives();
////        final FullLayerMapping fullLayerMapping = dockerImageDirectory.getLayerMapping(IMAGE_NAME, IMAGE_TAG);
////        assertEquals(2, fullLayerMapping.getManifestLayerMapping().getLayerInternalIds().size());
////        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
////        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(IMAGE_NAME, IMAGE_TAG));
////        final ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir);
////        tarParser.extractPkgMgrDb(ImageInspectorOsEnum.CENTOS, null, containerFileSystem, layerTars, fullLayerMapping, null);
////
////        // TODO this should be tested in a new ContainerFileSystemWithPkgMgrTest
////        LinuxDistroExtractor linuxDistroExtractor = new LinuxDistroExtractor(new FileOperations(), new Os());
////        PkgMgrDbExtractor pkgMgrDbExtractor = new PkgMgrDbExtractor(pkgMgrs, linuxDistroExtractor);
////        ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = pkgMgrDbExtractor.extract(containerFileSystem, null);
////        assertEquals("image_blackducksoftware_centos_minus_vim_plus_bacula_v_1.0", containerFileSystemWithPkgMgrDb.getTargetImageFileSystem().getTargetImageFileSystemFull().getName());
////        assertEquals("RPM", containerFileSystemWithPkgMgrDb.getPkgMgr().getType().name());
////
////        boolean varLibRpmNameFound = false;
////        final Collection<File> files = FileUtils.listFiles(workingDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
////        int numFilesFound = files.size();
////        for (final File file : files) {
////            if (file.getAbsolutePath().endsWith("var/lib/rpm/Name")) {
////                System.out.println(file.getAbsolutePath());
////                varLibRpmNameFound = true;
////                final List<String> cmd = Arrays.asList("strings", file.getAbsolutePath());
////                final String[] cmdOutput = (new CmdExecutor()).executeCommand(cmd, 30000L);
////                final String stringsOutput = Arrays.asList(cmdOutput).stream().collect(Collectors.joining("\n"));
////                assertTrue(stringsOutput.contains("bacula-console"));
////                assertTrue(stringsOutput.contains("bacula-client"));
////                assertTrue(stringsOutput.contains("bacula-director"));
////                break;
////            }
////        }
////        assertTrue(varLibRpmNameFound);
////
////        // MacOS file system does not preserve case which throws off the count
////        System.out.printf("Extracted %d files\n", numFilesFound);
////        assertTrue(numFilesFound > 10000);
////        assertTrue(numFilesFound < 19000);
////    }
//
//    @Test
//    public void testExtractDockerLayerTarSimple() throws WrongInspectorOsException, IOException {
//        doLayerTest("simple");
//    }
//
//    private void doLayerTest(final String testFileDir) throws WrongInspectorOsException, IOException {
//        final File workingDirectory = TestUtils.createTempDirectory();
//        final File tarExtractionDirectory = new File(workingDirectory, ImageInspector.TAR_EXTRACTION_DIRECTORY);
//        final File layerDir = new File(tarExtractionDirectory, String.format("ubuntu_latest.tar/%s", LAYER_ID));
//        layerDir.mkdirs();
//        final Path layerDirPath = Paths.get(layerDir.getAbsolutePath());
//        assertEquals("layerId1", layerDirPath.getFileName().toString());
//
//        final File dockerTar = new File(layerDir, "layer.tar");
//        Files.copy(new File(String.format("src/test/resources/%s/layer.tar", testFileDir)).toPath(), dockerTar.toPath(), REPLACE_EXISTING);
//        final List<TypedArchiveFile> layerTars = new ArrayList<>();
//        layerTars.add(new TypedArchiveFile(ArchiveFileType.TAR, dockerTar));
//
//        final DockerTarParser tarParser = new DockerTarParser();
//        tarParser.setManifestFactory(new DockerManifestFactory());
//        tarParser.setFileOperations(new FileOperations());
//        tarParser.setPkgMgrs(pkgMgrs);
//        tarParser.setPkgMgrExecutor(new PkgMgrExecutor());
//        tarParser.setDockerLayerTarExtractor(new DockerLayerTarExtractor());
//        tarParser.setLayerConfigParser(new DockerLayerConfigParser(new Gson()));
//
//        final List<String> layerIds = new ArrayList<>();
//        layerIds.add(LAYER_ID);
//        final ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(IMAGE_NAME, IMAGE_TAG, "test config filename", layerIds);
//        FullLayerMapping fullLayerMapping = new FullLayerMapping(manifestLayerMapping, new ArrayList<>());
//
//        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
//        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(IMAGE_NAME, IMAGE_TAG));
//        final ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir);
//        tarParser.extractPkgMgrDb(ImageInspectorOsEnum.UBUNTU, null, containerFileSystem, layerTars, fullLayerMapping, null);
//        assertEquals(tarExtractionDirectory.getAbsolutePath() + String.format("/imageFiles/%s", targetImageFileSystemRootDir.getName()), targetImageFileSystemRootDir.getAbsolutePath());
//
//        final File dpkgStatusFile = new File(workingDirectory.getAbsolutePath() + String.format("/tarExtraction/imageFiles/%s/var/lib/dpkg/status", targetImageFileSystemRootDir.getName()));
//        assertTrue(dpkgStatusFile.exists());
//
//        assertEquals(DPKG_STATUS_FILE_SIZE, FileUtils.sizeOf(dpkgStatusFile));
//    }
//
//    @Test
//    public void testCreateInitialImageComponentHierarchy() throws IntegrationException {
//        File workingDir = new File("build/images/test/alpine");
//        final File tarExtractionDirectory = new File(workingDir, ImageInspector.TAR_EXTRACTION_DIRECTORY);
//        String tarFilename = "alpine.tar";
//
//        final DockerTarParser tarParser = new DockerTarParser();
//        tarParser.setManifestFactory(new DockerManifestFactory());
//        tarParser.setFileOperations(new FileOperations());
//        tarParser.setPkgMgrs(pkgMgrs);
//        DockerImageConfigParser dockerImageConfigParser = new DockerImageConfigParser();
//        tarParser.setImageConfigParser(dockerImageConfigParser);
//        // TODO reuse a Gson
//        tarParser.setLayerConfigParser(new DockerLayerConfigParser(new Gson()));
//        File imageDir = new File(tarExtractionDirectory, tarFilename);
//        DockerImageDirectory dockerImageDirectory = new DockerImageDirectory(new Gson(), new FileOperations(), new DockerImageConfigParser(), new DockerManifestFactory(), imageDir);
//        FullLayerMapping mapping = dockerImageDirectory.getLayerMapping("alpine", "latest");
//        assertEquals("alpine", mapping.getManifestLayerMapping().getImageName());
//        assertEquals("latest", mapping.getManifestLayerMapping().getTagName());
//        assertTrue(mapping.getLayerExternalId(0).startsWith("sha256:"));
//    }
//
//    private void testParseImageInfo(String imageInspectorDistro, PackageManagerEnum packageManagerType, String pkgMgrDirName)
//        throws PkgMgrDataNotFoundException {
//
//        final DockerTarParser tarParser = new DockerTarParser();
//        tarParser.setManifestFactory(new DockerManifestFactory());
//        tarParser.setOs(new Os());
//        tarParser.setFileOperations(new FileOperations());
//        tarParser.setPkgMgrs(pkgMgrs);
//
//        final File containerFilesystemRoot = new File(String.format("src/test/resources/imageDir/%s", imageInspectorDistro));
//        final ContainerFileSystem containerFileSystem = new ContainerFileSystem(containerFilesystemRoot);
//
//        // TODO this should be tested elsewhere
//        LinuxDistroExtractor linuxDistroExtractor = new LinuxDistroExtractor(new FileOperations(), new Os());
//        PkgMgrDbExtractor pkgMgrDbExtractor = new PkgMgrDbExtractor(pkgMgrs, linuxDistroExtractor);
//        ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = pkgMgrDbExtractor.extract(containerFileSystem, null);
//        assertEquals(imageInspectorDistro, containerFileSystemWithPkgMgrDb.getLinuxDistroName());
//        assertEquals(packageManagerType, containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getPackageManager());
//        assertEquals(pkgMgrDirName, containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getExtractedPackageManagerDirectory().getName());
//        assertEquals(imageInspectorDistro, containerFileSystemWithPkgMgrDb.getTargetImageFileSystem().getTargetImageFileSystemFull().getName());
//    }
//}
