package com.blackduck.integration.blackduck.imageinspector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.blackduck.integration.blackduck.imageinspector.ImageInspector;
import com.blackduck.integration.blackduck.imageinspector.TestUtils;
import com.blackduck.integration.blackduck.imageinspector.bdio.BdioGenerator;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.blackduck.integration.bdio.model.BdioComponent;
import com.blackduck.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemCompatibilityChecker;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.LinuxDistroExtractor;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.PackageGetter;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.PkgMgrDbExtractor;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentHierarchyBuilder;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchyLogger;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrExecutor;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk.ApkPkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.dpkg.DpkgPkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.CommonRelationshipPopulater;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.rpm.RpmPkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.rpm.RpmRelationshipPopulater;
import com.blackduck.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractorFactoryChooser;
import com.blackduck.integration.blackduck.imageinspector.image.common.ImageLayerApplier;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.ImageLayerArchiveExtractor;
import com.blackduck.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.linux.Os;
import com.blackduck.integration.blackduck.imageinspector.linux.TarOperations;
import com.blackduck.integration.exception.IntegrationException;

@Tag("integration")
public class ImageInspectorApiIntTest {
    private static final String PROJECT_VERSION = "unitTest1";
    private static final String PROJECT = "SB001";
    private static final String SIMPLE_IMAGE_TARFILE = "build/images/test/alpine.tar";
    private static final String MULTILAYER_IMAGE_TARFILE = "build/images/test/centos_minus_vim_plus_bacula.tar";
    private static final String NOPKGMGR_IMAGE_TARFILE = "build/images/test/nopkgmgr.tar";
    private static final String OCI_IMAGE_TARFILE = "src/test/resources/oci/u_multi_tagged_gutted.tar";
    private static final String IMAGE_TARFILE_GZIPPED = "src/test/resources/gutted.tar.gz";

    private Os os;
    private ImageInspectorApi imageInspectorApi;
    private List<PkgMgr> pkgMgrs;
    private PackageGetter packageGetter;
    private RpmPkgMgr rpmPkgMgr;
    private DpkgPkgMgr dpkgPkgMgr;

    private static final String[] apkOutput = { "ca-certificates-20171114-r0", "boost-unit_test_framework-1.62.0-r5" };

    @TempDir
    File tempDir;

    @BeforeEach
    public void setup() throws IntegrationException, InterruptedException {
        dpkgPkgMgr = Mockito.mock(DpkgPkgMgr.class);
        List<ComponentDetails> ubuntuComps = new ArrayList<>();
        ubuntuComps.add(new ComponentDetails("comp0", "version0", "testExternalId0", "testArch", "ubuntu"));
        Mockito.when(dpkgPkgMgr.extractComponentsFromPkgMgrOutput(Mockito.any(File.class), Mockito.anyString(), Mockito.any(String[].class))).thenReturn(ubuntuComps);
        Mockito.when(dpkgPkgMgr.getType()).thenReturn(PackageManagerEnum.DPKG);
        Mockito.when(dpkgPkgMgr.getImagePackageManagerDirectory(Mockito.any(File.class))).thenReturn(new File("."));

        rpmPkgMgr = Mockito.mock(RpmPkgMgr.class);
        List<ComponentDetails> centosComps = new ArrayList<>();
        centosComps.add(new ComponentDetails("comp0", "version0", "testExternalId0", "testArch", "centos"));
        Mockito.when(rpmPkgMgr.extractComponentsFromPkgMgrOutput(Mockito.any(File.class), Mockito.anyString(), Mockito.any(String[].class))).thenReturn(centosComps);

        FileOperations fileOperations = new FileOperations();
        pkgMgrs = new ArrayList<>(3);
        pkgMgrs.add(new ApkPkgMgr(fileOperations));
        pkgMgrs.add(dpkgPkgMgr);
        pkgMgrs.add(rpmPkgMgr);
        os = Mockito.mock(Os.class);

        LinuxDistroExtractor linuxDistroExtractor = new LinuxDistroExtractor(fileOperations, os);
        PkgMgrDbExtractor pkgMgrDbExtractor = new PkgMgrDbExtractor(pkgMgrs, linuxDistroExtractor);
        PkgMgrExecutor pkgMgrExecutor = Mockito.mock(PkgMgrExecutor.class);
        CmdExecutor cmdExecutor = Mockito.mock(CmdExecutor.class);
        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(CmdExecutor.class), Mockito.any(ApkPkgMgr.class), Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(apkOutput);

        packageGetter = new PackageGetter(pkgMgrExecutor, cmdExecutor);

        TarOperations tarOperations = new TarOperations();
        tarOperations.setFileOperations(fileOperations);
        ContainerFileSystemCompatibilityChecker containerFileSystemCompatibilityChecker = new ContainerFileSystemCompatibilityChecker();
        ImageLayerApplier imageLayerApplier = new ImageLayerApplier(fileOperations, new ImageLayerArchiveExtractor());
        ImageInspector imageInspector = new ImageInspector(os, pkgMgrDbExtractor, tarOperations,
            new FileOperations(), imageLayerApplier,
            containerFileSystemCompatibilityChecker, new BdioGenerator(),
            new ImageDirectoryDataExtractorFactoryChooser(),
            new ImageComponentHierarchyLogger()
        );
        imageInspectorApi = new ImageInspectorApi(imageInspector, os);
        imageInspectorApi.setFileOperations(new FileOperations());
        imageInspectorApi.setBdioGenerator(TestUtils.createBdioGenerator());
        imageInspectorApi.setPkgMgrExecutor(pkgMgrExecutor);
        imageInspectorApi.setCmdExecutor(cmdExecutor);
    }

    @Test
    public void testOnWrongOs() throws IntegrationException, InterruptedException {
        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        // currently-running-on distro:
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.CENTOS);
        // target image distro
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("alpine"));
        try {
            ImageInspectionRequest imageInspectionRequest = new ImageInspectionRequestBuilder()
                .setDockerTarfilePath(SIMPLE_IMAGE_TARFILE)
                .setBlackDuckProjectName(PROJECT)
                .setBlackDuckProjectVersion(PROJECT_VERSION)
                .setOrganizeComponentsByLayer(false)
                .setIncludeRemovedComponents(false)
                .setCurrentLinuxDistro("CENTOS")
                .setCleanupWorkingDir(true)
                .build();
            imageInspectorApi.getBdio(componentHierarchyBuilder, imageInspectionRequest);
            fail("Expected WrongInspectorOsException");
        } catch (WrongInspectorOsException e) {
            System.out.println(String.format(
                "Can't inspect on this OS; need to inspect on %s",
                e.getcorrectInspectorOs() == null ? "<unknown>" : e.getcorrectInspectorOs().name()
            ));
            assertEquals(ImageInspectorOsEnum.ALPINE.name(), e.getcorrectInspectorOs().name());
        }
    }

    @Test
    public void testOnNoPkgMgrImage() throws IntegrationException, InterruptedException, IOException {
        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.UBUNTU);

        File destinationFile = new File(tempDir, "out.tar.gz");
        String containerFileSystemOutputFilePath = destinationFile.getAbsolutePath();

        ImageInspectionRequest imageInspectionRequest = new ImageInspectionRequestBuilder()
            .setDockerTarfilePath(NOPKGMGR_IMAGE_TARFILE)
            .setBlackDuckProjectName(PROJECT)
            .setBlackDuckProjectVersion(PROJECT_VERSION)
            .setOrganizeComponentsByLayer(false)
            .setIncludeRemovedComponents(false)
            .setCurrentLinuxDistro("UBUNTU")
            .setContainerFileSystemOutputPath(containerFileSystemOutputFilePath)
            .setCleanupWorkingDir(true)
            .build();
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(componentHierarchyBuilder, imageInspectionRequest);
        assertEquals(0, bdioDocument.getComponents().size());

        File containerFileSystemFile = new File(containerFileSystemOutputFilePath);
        System.out.printf("output file: %s\n", containerFileSystemFile.getAbsolutePath());
        assertTrue(containerFileSystemFile.length() > 0);
        assertTrue(containerFileSystemFile.length() < 1000);
    }

    @Test
    public void testAppOnlyFileSystem() throws IntegrationException, IOException, InterruptedException {
        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("centos"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.CENTOS);
        Mockito.when(rpmPkgMgr.isApplicable(Mockito.any(File.class))).thenReturn(true);
        Mockito.when(rpmPkgMgr.getType()).thenReturn(PackageManagerEnum.RPM);
        Mockito.when(rpmPkgMgr.getImagePackageManagerDirectory(Mockito.any(File.class))).thenReturn(new File("."));

        CmdExecutor cmdExecutor = Mockito.mock(CmdExecutor.class);
        Mockito.when(cmdExecutor.executeCommand(Mockito.any(List.class), Mockito.any(Long.class))).thenReturn(new String[0]);
        Mockito.when(rpmPkgMgr.createRelationshipPopulator(Mockito.any(CmdExecutor.class))).thenReturn(new RpmRelationshipPopulater(cmdExecutor));

        File destinationFile = new File(tempDir, "out.tar.gz");
        String containerFileSystemOutputFilePath = destinationFile.getAbsolutePath();
        ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
            .setDockerTarfilePath(MULTILAYER_IMAGE_TARFILE)
            .setBlackDuckProjectName(PROJECT)
            .setBlackDuckProjectVersion(PROJECT_VERSION)
            .setContainerFileSystemOutputPath(containerFileSystemOutputFilePath)
            .setCurrentLinuxDistro("CENTOS")
            .setPlatformTopLayerExternalId("sha256:0e07d0d4c60c0a54ad297763c829584b15d1a4a848bf21fb69dc562feee5bf11")
            .setCleanupWorkingDir(true)
            .build();
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(componentHierarchyBuilder, imageInspectionRequest);

        File containerFileSystemFile = new File(containerFileSystemOutputFilePath);
        System.out.printf("output file: %s\n", containerFileSystemFile.getAbsolutePath());

        ImageLayerArchiveExtractor archiveExtractor = new ImageLayerArchiveExtractor();
        File extractedFilesDir = new File(tempDir, "extracted");
        archiveExtractor.extractLayerGzipTarToDir(new FileOperations(), containerFileSystemFile, extractedFilesDir);

        File[] extractedFiles = extractedFilesDir.listFiles();
        assertEquals(1, extractedFiles.length);

        File appLayersDir = extractedFiles[0];
        File etcDir = new File(appLayersDir, "etc");
        assertTrue(etcDir.exists());

        File homeDir = new File(appLayersDir, "home");
        assertFalse(homeDir.exists());
    }

    @Test
    public void testOciImageRepoTagNotSpecified() throws IntegrationException, IOException, InterruptedException {

        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("ubuntu"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.UBUNTU);
        Mockito.when(dpkgPkgMgr.isApplicable(Mockito.any(File.class))).thenReturn(true);
        Mockito.when(dpkgPkgMgr.createRelationshipPopulator(Mockito.any(CmdExecutor.class))).thenReturn(new CommonRelationshipPopulater(null));

        File destinationFile = new File(tempDir, "out.tar.gz");
        String containerFileSystemOutputFilePath = destinationFile.getAbsolutePath();
        ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
            .setDockerTarfilePath(OCI_IMAGE_TARFILE)
            .setContainerFileSystemOutputPath(containerFileSystemOutputFilePath)
            .setCurrentLinuxDistro("UBUNTU")
            .setCleanupWorkingDir(true)
            .build();
        SimpleBdioDocument bdioDocument;
        try {
            bdioDocument = imageInspectorApi.getBdio(componentHierarchyBuilder, imageInspectionRequest);
            fail("Expected exception");
        } catch (IntegrationException e) {
            assertTrue(e.getMessage().contains("multiple manifests"));
        }
    }

    @Test
    public void testOciImageRepoTagSpecified() throws IntegrationException, IOException, InterruptedException {

        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("ubuntu"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.UBUNTU);
        Mockito.when(dpkgPkgMgr.isApplicable(Mockito.any(File.class))).thenReturn(true);
        Mockito.when(dpkgPkgMgr.createRelationshipPopulator(Mockito.any(CmdExecutor.class)))
            .thenReturn(new CommonRelationshipPopulater(new DbRelationshipInfo(new HashMap<>(), new HashMap<>())));

        File destinationFile = new File(tempDir, "out.tar.gz");
        String containerFileSystemOutputFilePath = destinationFile.getAbsolutePath();
        ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
            .setDockerTarfilePath(OCI_IMAGE_TARFILE)
            .setGivenImageRepo("testrepo")
            .setGivenImageTag("testtag")
            .setContainerFileSystemOutputPath(containerFileSystemOutputFilePath)
            .setCurrentLinuxDistro("UBUNTU")
            .setCleanupWorkingDir(true)
            .build();
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(componentHierarchyBuilder, imageInspectionRequest);
        assertEquals("testrepo", bdioDocument.getProject().name);
        assertEquals("testtag", bdioDocument.getProject().version);

        File containerFileSystemFile = new File(containerFileSystemOutputFilePath);
        assertTrue(containerFileSystemFile.exists());
    }

    @Test
    public void testTarDotGz() throws IntegrationException, IOException, InterruptedException {

        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("ubuntu"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.UBUNTU);
        Mockito.when(dpkgPkgMgr.isApplicable(Mockito.any(File.class))).thenReturn(true);
        Mockito.when(dpkgPkgMgr.createRelationshipPopulator(Mockito.any(CmdExecutor.class))).thenReturn(new CommonRelationshipPopulater(null));

        ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
            .setDockerTarfilePath(IMAGE_TARFILE_GZIPPED)
            .setCurrentLinuxDistro("UBUNTU")
            .setCleanupWorkingDir(true)
            .build();
        try {
            imageInspectorApi.getBdio(componentHierarchyBuilder, imageInspectionRequest);
            fail("Expected exception");
        } catch (InvalidArchiveFormatException e) {
            assertTrue(e.getMessage().contains("UNIX tar"));
        }
    }

    @Test
    public void testOnRightOs() throws IntegrationException, InterruptedException {
        doAlpineTest(null);
    }

    @Test
    public void testOnRightOsDistroOverride() throws IntegrationException, InterruptedException {
        doAlpineTest("overriddendistroname");
    }

    private void doAlpineTest(String targetLinuxDistroOverride) throws IntegrationException, InterruptedException {
        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        String expectedForgeName;
        if (StringUtils.isNotBlank(targetLinuxDistroOverride)) {
            expectedForgeName = "@" + targetLinuxDistroOverride;
        } else {
            expectedForgeName = "@alpine";
        }
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("alpine"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.ALPINE);
        ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
            .setDockerTarfilePath(SIMPLE_IMAGE_TARFILE)
            .setBlackDuckProjectName(PROJECT)
            .setBlackDuckProjectVersion(PROJECT_VERSION)
            .setCurrentLinuxDistro("ALPINE")
            .setTargetLinuxDistroOverride(targetLinuxDistroOverride)
            .setCleanupWorkingDir(true)
            .build();
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(componentHierarchyBuilder, imageInspectionRequest);
        System.out.printf("bdioDocument: %s\n", bdioDocument);
        assertEquals(PROJECT, bdioDocument.getProject().name);
        assertEquals(PROJECT_VERSION, bdioDocument.getProject().version);
        assertEquals(apkOutput.length, bdioDocument.getComponents().size());
        for (BdioComponent comp : bdioDocument.getComponents()) {
            System.out.printf("comp: %s:%s:%s\n", comp.name, comp.version, comp.bdioExternalIdentifier.externalId);
            if (comp.name.equals("boost-unit_test_framework")) {
                assertEquals("1.62.0-r5", comp.version);
                assertEquals("boost-unit_test_framework/1.62.0-r5/x86_64", comp.bdioExternalIdentifier.externalId);
                assertEquals(expectedForgeName, comp.bdioExternalIdentifier.forge);
            } else {
                assertEquals("ca-certificates", comp.name);
                assertEquals("20171114-r0", comp.version);
                assertEquals("ca-certificates/20171114-r0/x86_64", comp.bdioExternalIdentifier.externalId);
                assertEquals(expectedForgeName, comp.bdioExternalIdentifier.forge);
            }
        }
    }
}
