package com.synopsys.integration.blackduck.imageinspector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.compressors.CompressorException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerLayerTarExtractor;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.LayerConfigParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm.RpmPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class ImageInspectorApiIntTest {

    private static final String PROJECT_VERSION = "unitTest1";

    private static final String PROJECT = "SB001";

    private static final String IMAGE_TARFILE = "build/images/test/alpine.tar";
    
    private static Os os;
    private static ImageInspectorApi imageInspectorApi;
    private static List<PkgMgr> pkgMgrs;

    private static String[] apkOutput = { "ca-certificates-20171114-r0", "boost-unit_test_framework-1.62.0-r5" };

    @BeforeAll
    public static void setup() throws IntegrationException {
        final FileOperations fileOperations = new FileOperations();
        pkgMgrs = new ArrayList<>(3);
        pkgMgrs.add(new ApkPkgMgr(fileOperations));
        pkgMgrs.add(new DpkgPkgMgr(fileOperations));
        pkgMgrs.add(new RpmPkgMgr(new Gson(), fileOperations));
        os = Mockito.mock(Os.class);

        final PkgMgrExecutor pkgMgrExecutor = Mockito.mock(PkgMgrExecutor.class);
        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(CmdExecutor.class), Mockito.any(PkgMgr.class), Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(apkOutput);

        final DockerTarParser dockerTarParser = new DockerTarParser();
        dockerTarParser.setManifestFactory(new ManifestFactory());
        dockerTarParser.setOs(os);
        dockerTarParser.setFileOperations(new FileOperations());
        dockerTarParser.setPkgMgrs(pkgMgrs);
        dockerTarParser.setPkgMgrExecutor(pkgMgrExecutor);
        dockerTarParser.setDockerLayerTarExtractor(new DockerLayerTarExtractor());
        dockerTarParser.setImageConfigParser(new ImageConfigParser());
        dockerTarParser.setLayerConfigParser(new LayerConfigParser());
        final PkgMgrFactory pkgMgrFactory = new PkgMgrFactory();
        final ImageInspector imageInspector = new ImageInspector(dockerTarParser);
        imageInspectorApi = new ImageInspectorApi(imageInspector, os);
        imageInspectorApi.setFileOperations(new FileOperations());
        imageInspectorApi.setBdioGenerator(new BdioGenerator());
    }

    @Test
    public void testOnWrongOs() throws IntegrationException, IOException, InterruptedException, CompressorException {
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.CENTOS);
        try {
            imageInspectorApi.getBdio(IMAGE_TARFILE, PROJECT, PROJECT_VERSION, null, null, null, false, false, false, null, "CENTOS", null);
            fail("Expected WrongInspectorOsException");
        } catch (final WrongInspectorOsException e) {
            System.out.println(String.format("Can't inspect on this OS; need to inspect on %s", e.getcorrectInspectorOs() == null ? "<unknown>" : e.getcorrectInspectorOs().name()));
            assertEquals(ImageInspectorOsEnum.ALPINE.name(), e.getcorrectInspectorOs().name());
        }
    }

    @Test
    public void testOnRightOs() throws IntegrationException, IOException, InterruptedException, CompressorException {
        Mockito.when(os.isLinuxDistroFile(Mockito.any(File.class))).thenReturn(Boolean.TRUE);
        Mockito.when(os.getLinxDistroName(Mockito.any(File.class))).thenReturn(Optional.of("alpine"));
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(ImageInspectorOsEnum.ALPINE);

        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(IMAGE_TARFILE, PROJECT, PROJECT_VERSION, null, null, null, false, false, false, null, "ALPINE", null);
        System.out.printf("bdioDocument: %s\n", bdioDocument);
        assertEquals(PROJECT, bdioDocument.project.name);
        assertEquals(PROJECT_VERSION, bdioDocument.project.version);
        assertEquals(apkOutput.length, bdioDocument.components.size());
        for (BdioComponent comp : bdioDocument.components) {
            System.out.printf("comp: %s:%s:%s\n", comp.name, comp.version, comp.bdioExternalIdentifier.externalId);
            if (comp.name.equals("boost-unit_test_framework")) {
                assertEquals("1.62.0-r5", comp.version);
                assertEquals("boost-unit_test_framework/1.62.0-r5/x86_64", comp.bdioExternalIdentifier.externalId);
            } else {
                assertEquals("ca-certificates", comp.name);
                assertEquals("20171114-r0", comp.version);
                assertEquals("ca-certificates/20171114-r0/x86_64", comp.bdioExternalIdentifier.externalId);
            }
        }
    }
}
