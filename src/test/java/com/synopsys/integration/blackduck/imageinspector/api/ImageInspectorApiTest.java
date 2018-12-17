package com.synopsys.integration.blackduck.imageinspector.api;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.synopsys.integration.bdio.model.BdioProject;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.exception.IntegrationException;
import java.io.IOException;
import org.apache.commons.compress.compressors.CompressorException;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

// TODO why does this fail?
@Disabled
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = { AppConfig.class })
public class ImageInspectorApiTest {

    private static final String CODE_LOCATION_PREFIX = "testCodeLocationPrefix";

    private static final String PROJECT_VERSION = "unitTest1";

    private static final String PROJECT = "SB001";

    private static final String TEST_ARCH = "testArch";

    private static final String IMAGE_TARFILE = "build/images/test/alpine.tar";

    private static final String MOCKED_PROJECT_ID = "mockedProjectId";
    private static final String IMAGE_REPO = "alpine";
    private static final String IMAGE_TAG = "latest";

    @Autowired
    private ImageInspectorApi imageInspectorApi;

    @MockBean
    private Os os;

    // TODO
    //@Test
    public void testOnWrongOs() throws IntegrationException, IOException, InterruptedException, CompressorException {
        assertNotNull(imageInspectorApi);
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(null);
        try {
            imageInspectorApi.getBdio(IMAGE_TARFILE, PROJECT, PROJECT_VERSION, null, null, null, false, false, false, null, null);
            fail("Expected WrongInspectorOsException");
        } catch (final WrongInspectorOsException e) {
            System.out.println(String.format("Can't inspect on this OS; need to inspect on %s", e.getcorrectInspectorOs() == null ? "<unknown>" : e.getcorrectInspectorOs().name()));
            assertEquals(OperatingSystemEnum.ALPINE.name(), e.getcorrectInspectorOs().name());
        }
    }

    // TODO
    //@Test
    public void testOnRightOs() throws IntegrationException, IOException, InterruptedException, CompressorException {
        assertNotNull(imageInspectorApi);
        Mockito.when(os.deriveOs(Mockito.any(String.class))).thenReturn(OperatingSystemEnum.ALPINE);
        final BdioGenerator mockExtractor = Mockito.mock(BdioGenerator.class);
        // Mockito.when(mockExtractor.getPackageManagerEnum()).thenReturn(PackageManagerEnum.APK);
        final SimpleBdioDocument mockedBdioDocument = new SimpleBdioDocument();
        mockedBdioDocument.project = new BdioProject();
        mockedBdioDocument.project.id = MOCKED_PROJECT_ID;
        // Mockito.when(mockExtractor.extract(Mockito.anyString(), Mockito.anyString(), Mockito.any(ImagePkgMgrDatabase.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        // .thenReturn(mockedBdioDocument);
        // Mockito.when(extractorManager.getExtractors()).thenReturn(mockExtractors);
        final ImageInfoDerived imageInfo = null; // imageInspectorApi.inspect(IMAGE_TARFILE, PROJECT, PROJECT_VERSION, CODE_LOCATION_PREFIX, null, null, false, false, null, null);
        assertEquals(String.format("%s_alpine_latest_lib_apk_APK", CODE_LOCATION_PREFIX), imageInfo.getCodeLocationName());
        assertEquals(PROJECT, imageInfo.getFinalProjectName());
        assertEquals(PROJECT_VERSION, imageInfo.getFinalProjectVersionName());
        assertEquals(String.format("image_%s_v_%s", IMAGE_REPO, IMAGE_TAG), imageInfo.getImageInfoParsed().getFileSystemRootDir().getName());
        assertEquals("/lib/apk", imageInfo.getImageInfoParsed().getPkgMgr().getPackageManager().getDirectory());
        assertEquals("apk", imageInfo.getImageInfoParsed().getPkgMgr().getExtractedPackageManagerDirectory().getName());
        assertEquals(IMAGE_TAG, imageInfo.getManifestLayerMapping().getTagName());
        assertEquals(IMAGE_REPO, imageInfo.getManifestLayerMapping().getImageName());
        assertEquals("8a6f655225cc4cf354cc4a21178983b10f835b82e12d300f328d9d5c59b2d872", imageInfo.getManifestLayerMapping().getLayers().get(0));
        assertEquals("lib/apk", imageInfo.getPkgMgrFilePath());

        final SimpleBdioDocument returnedBdioDocument = imageInfo.getBdioDocument();
        assertEquals(MOCKED_PROJECT_ID, returnedBdioDocument.project.id);
    }
}
