package com.synopsys.integration.blackduck.imageinspector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemCompatibilityChecker;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.LinuxDistroExtractor;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.PackageGetter;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.PkgMgrDbExtractor;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractorFactoryChooser;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchyLogger;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentHierarchyBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerApplier;
import com.synopsys.integration.blackduck.imageinspector.linux.TarOperations;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ImageLayerArchiveExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.rpm.RpmPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

/////////@Tag("integration")
public class ImageInspectorApiIntTest {
    private static final String PROJECT_VERSION = "unitTest1";
    private static final String PROJECT = "SB001";
    private static final String SIMPLE_IMAGE_TARFILE = "build/images/test/alpine.tar";
    private static final String MULTILAYER_IMAGE_TARFILE = "build/images/test/centos_minus_vim_plus_bacula.tar";
    private static final String NOPKGMGR_IMAGE_TARFILE = "build/images/test/nopkgmgr.tar";
    // TODO need to build this:
    private static final String OCI_IMAGE_TARFILE = "/tmp/ccc/centos_minus_vim_plus_bacula-oci.tar";

    private static Os os;
    private static ImageInspectorApi imageInspectorApi;
    private static List<PkgMgr> pkgMgrs;
    private static PackageGetter packageGetter;
    private static RpmPkgMgr rpmPkgMgr;

    private static final String[] apkOutput = { "ca-certificates-20171114-r0", "boost-unit_test_framework-1.62.0-r5" };

    @TempDir
    File tempDir;

    @BeforeAll
    public static void setup() throws IntegrationException, InterruptedException {
        rpmPkgMgr = Mockito.mock(RpmPkgMgr.class);
        List<ComponentDetails> comps = new ArrayList<>();
        comps.add(new ComponentDetails("comp0", "version0", "testExternalId0", "testArch", "centos"));
        Mockito.when(rpmPkgMgr.extractComponentsFromPkgMgrOutput(Mockito.any(File.class), Mockito.anyString(), Mockito.any(String[].class))).thenReturn(comps);
        FileOperations fileOperations = new FileOperations();
        pkgMgrs = new ArrayList<>(3);
        pkgMgrs.add(new ApkPkgMgr(fileOperations));
        pkgMgrs.add(new DpkgPkgMgr(fileOperations));
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
                new ImageComponentHierarchyLogger());
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
            System.out.println(String.format("Can't inspect on this OS; need to inspect on %s", e.getcorrectInspectorOs() == null ? "<unknown>" : e.getcorrectInspectorOs().name()));
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
    public void testOnRightOs() throws IntegrationException, InterruptedException {
        doTest(null);
    }

    @Test
    public void testOnRightOsDistroOverride() throws IntegrationException, InterruptedException {
        doTest("overriddendistroname");
    }

    private void doTest(String targetLinuxDistroOverride) throws IntegrationException, InterruptedException {
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

    @Test
    public void testAppOnlyFileSystem() throws IntegrationException, IOException, InterruptedException {
        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("centos"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.CENTOS);
        Mockito.when(rpmPkgMgr.isApplicable(Mockito.any(File.class))).thenReturn(true);
        Mockito.when(rpmPkgMgr.getType()).thenReturn(PackageManagerEnum.RPM);
        Mockito.when(rpmPkgMgr.getImagePackageManagerDirectory(Mockito.any(File.class))).thenReturn(new File("."));

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
        assertTrue(containerFileSystemFile.length() > 10000000);
        assertTrue(containerFileSystemFile.length() < 80000000);
    }

    @Test
    public void testOciImage() throws IntegrationException, IOException, InterruptedException {
        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("centos"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.CENTOS);
        Mockito.when(rpmPkgMgr.isApplicable(Mockito.any(File.class))).thenReturn(true);
        Mockito.when(rpmPkgMgr.getType()).thenReturn(PackageManagerEnum.RPM);
        Mockito.when(rpmPkgMgr.getImagePackageManagerDirectory(Mockito.any(File.class))).thenReturn(new File("."));

        File destinationFile = new File(tempDir, "out.tar.gz");
        String containerFileSystemOutputFilePath = destinationFile.getAbsolutePath();
        ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
                .setDockerTarfilePath(OCI_IMAGE_TARFILE)
                //.setBlackDuckProjectName(PROJECT)
                //.setBlackDuckProjectVersion(PROJECT_VERSION)
                .setContainerFileSystemOutputPath(containerFileSystemOutputFilePath)
                .setCurrentLinuxDistro("CENTOS")
                .setCleanupWorkingDir(true)
                .build();
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(componentHierarchyBuilder, imageInspectionRequest);

        File containerFileSystemFile = new File(containerFileSystemOutputFilePath);
        System.out.printf("output file: %s\n", containerFileSystemFile.getAbsolutePath());
        assertTrue(containerFileSystemFile.length() > 10000000);
    }
}
