package com.synopsys.integration.blackduck.imageinspector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.linux.TarOperations;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerLayerTarExtractor;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.LayerConfigParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm.RpmPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class ImageInspectorApiIntTest {
    private static final String PROJECT_VERSION = "unitTest1";
    private static final String PROJECT = "SB001";
    private static final String SIMPLE_IMAGE_TARFILE = "build/images/test/alpine.tar";
    private static final String MULTILAYER_IMAGE_TARFILE = "build/images/test/centos_minus_vim_plus_bacula.tar";
    private static final String NOPKGMGR_IMAGE_TARFILE = "build/images/test/nopkgmgr.tar";

    private static Os os;
    private static ImageInspectorApi imageInspectorApi;
    private static List<PkgMgr> pkgMgrs;

    private static final String[] apkOutput = { "ca-certificates-20171114-r0", "boost-unit_test_framework-1.62.0-r5" };

    @BeforeAll
    public static void setup() throws IntegrationException, InterruptedException {
        FileOperations fileOperations = new FileOperations();
        pkgMgrs = new ArrayList<>(3);
        pkgMgrs.add(new ApkPkgMgr(fileOperations));
        pkgMgrs.add(new DpkgPkgMgr(fileOperations));
        pkgMgrs.add(new RpmPkgMgr(new Gson(), fileOperations));
        os = Mockito.mock(Os.class);

        PkgMgrExecutor pkgMgrExecutor = Mockito.mock(PkgMgrExecutor.class);
        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(CmdExecutor.class), Mockito.any(PkgMgr.class), Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(apkOutput);

        DockerTarParser dockerTarParser = new DockerTarParser();
        dockerTarParser.setManifestFactory(new ManifestFactory());
        dockerTarParser.setOs(os);
        dockerTarParser.setFileOperations(new FileOperations());
        dockerTarParser.setPkgMgrs(pkgMgrs);
        dockerTarParser.setPkgMgrExecutor(pkgMgrExecutor);
        dockerTarParser.setDockerLayerTarExtractor(new DockerLayerTarExtractor());
        dockerTarParser.setImageConfigParser(new ImageConfigParser());
        dockerTarParser.setLayerConfigParser(new LayerConfigParser());
        PkgMgrFactory pkgMgrFactory = new PkgMgrFactory();
        TarOperations tarOperations = new TarOperations();
        tarOperations.setFileOperations(fileOperations);
        ImageInspector imageInspector = new ImageInspector(dockerTarParser, tarOperations);
        imageInspectorApi = new ImageInspectorApi(imageInspector, os);
        imageInspectorApi.setFileOperations(new FileOperations());
        imageInspectorApi.setBdioGenerator(TestUtils.createBdioGenerator());
    }

    @Test
    public void testOnWrongOs() throws IntegrationException, InterruptedException {
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.CENTOS);
        try {
            ImageInspectionRequest imageInspectionRequest = new ImageInspectionRequestBuilder()
                                                                .setDockerTarfilePath(SIMPLE_IMAGE_TARFILE)
                                                                .setBlackDuckProjectName(PROJECT)
                                                                .setBlackDuckProjectVersion(PROJECT_VERSION)
                                                                .setOrganizeComponentsByLayer(false)
                                                                .setIncludeRemovedComponents(false)
                                                                .setCurrentLinuxDistro("CENTOS")
                                                                .build();
            imageInspectorApi.getBdio(imageInspectionRequest);
            fail("Expected WrongInspectorOsException");
        } catch (WrongInspectorOsException e) {
            System.out.println(String.format("Can't inspect on this OS; need to inspect on %s", e.getcorrectInspectorOs() == null ? "<unknown>" : e.getcorrectInspectorOs().name()));
            assertEquals(ImageInspectorOsEnum.ALPINE.name(), e.getcorrectInspectorOs().name());
        }
    }

    @Test
    public void testOnNoPkgMgrImage() throws IntegrationException, InterruptedException, IOException {
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.UBUNTU);

        FileOperations fileOperations = new FileOperations();
        File tempDir = fileOperations.createTempDirectory();
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
                                                            .build();
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(imageInspectionRequest);
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
                                                            .build();
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(imageInspectionRequest);
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
        Mockito.when(os.getLinuxDistroNameFromEtcDir(Mockito.any(File.class))).thenReturn(Optional.of("centos"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.CENTOS);

        FileOperations fileOperations = new FileOperations();
        File tempDir = fileOperations.createTempDirectory();
        File destinationFile = new File(tempDir, "out.tar.gz");
        String containerFileSystemOutputFilePath = destinationFile.getAbsolutePath();
        ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
                                                            .setDockerTarfilePath(MULTILAYER_IMAGE_TARFILE)
                                                            .setBlackDuckProjectName(PROJECT)
                                                            .setBlackDuckProjectVersion(PROJECT_VERSION)
                                                            .setContainerFileSystemOutputPath(containerFileSystemOutputFilePath)
                                                            .setCurrentLinuxDistro("CENTOS")
                                                            .setPlatformTopLayerExternalId("sha256:0e07d0d4c60c0a54ad297763c829584b15d1a4a848bf21fb69dc562feee5bf11")
                                                            .build();
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(imageInspectionRequest);

        File containerFileSystemFile = new File(containerFileSystemOutputFilePath);
        System.out.printf("output file: %s\n", containerFileSystemFile.getAbsolutePath());
        assertTrue(containerFileSystemFile.length() > 10000000);
        assertTrue(containerFileSystemFile.length() < 80000000);
    }
}
