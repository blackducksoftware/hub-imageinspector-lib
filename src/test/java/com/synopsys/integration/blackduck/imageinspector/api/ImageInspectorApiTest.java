package com.synopsys.integration.blackduck.imageinspector.api;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ApkExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.exception.IntegrationException;
import java.io.IOException;
import org.apache.commons.compress.compressors.CompressorException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("integration")
public class ImageInspectorApiTest {

    private static final String CODE_LOCATION_PREFIX = "testCodeLocationPrefix";

    private static final String PROJECT_VERSION = "unitTest1";

    private static final String PROJECT = "SB001";

    private static final String TEST_ARCH = "testArch";

    private static final String IMAGE_TARFILE = "build/images/test/alpine.tar";

    private static final String MOCKED_PROJECT_ID = "mockedProjectId";
    private static final String IMAGE_REPO = "alpine";
    private static final String IMAGE_TAG = "latest";


    private static Os os;
    private static ImageInspectorApi imageInspectorApi;
    private static ApkExecutor apkExecutor;

    @BeforeAll
    public static void setup() {
        os = Mockito.mock(Os.class);
        final DockerTarParser dockerTarParser = new DockerTarParser();
        dockerTarParser.setManifestFactory(new ManifestFactory());
        dockerTarParser.setOs(os);
        final ComponentExtractorFactory componentExtractorFactory = new ComponentExtractorFactory();
        apkExecutor = Mockito.mock(ApkExecutor.class);
        componentExtractorFactory.setApkExecutor(apkExecutor);
        final ImageInspector imageInspector = new ImageInspector(dockerTarParser, componentExtractorFactory);
        imageInspectorApi = new ImageInspectorApi(imageInspector, os);
    }

    @Test
    public void testOnWrongOs() throws IntegrationException, IOException, InterruptedException, CompressorException {
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(OperatingSystemEnum.CENTOS);
        try {
            imageInspectorApi.getBdio(IMAGE_TARFILE, PROJECT, PROJECT_VERSION, null, null, null, false, false, false, null, "CENTOS");
            fail("Expected WrongInspectorOsException");
        } catch (final WrongInspectorOsException e) {
            System.out.println(String.format("Can't inspect on this OS; need to inspect on %s", e.getcorrectInspectorOs() == null ? "<unknown>" : e.getcorrectInspectorOs().name()));
            assertEquals(OperatingSystemEnum.ALPINE.name(), e.getcorrectInspectorOs().name());
        }
    }

    @Test
    public void testOnRightOs() throws IntegrationException, IOException, InterruptedException, CompressorException {
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(OperatingSystemEnum.ALPINE);
        String[] apkPackages = { "ca-certificates-20171114-r0", "boost-unit_test_framework-1.62.0-r5" };
        Mockito.when(apkExecutor.runPackageManager(Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(apkPackages);
        SimpleBdioDocument bdioDocument = imageInspectorApi.getBdio(IMAGE_TARFILE, PROJECT, PROJECT_VERSION, null, null, null, false, false, false, null, "ALPINE");
        System.out.printf("bdioDocument: %s\n", bdioDocument);
        assertEquals(PROJECT, bdioDocument.project.name);
        assertEquals(PROJECT_VERSION, bdioDocument.project.version);
        assertEquals(apkPackages.length, bdioDocument.components.size());
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
