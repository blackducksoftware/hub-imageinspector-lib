package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

class ImageInspectorTest {
    private DockerTarParser tarParser;
    private ImageInspector imageInspector;

    @BeforeEach
    public void setUpEach() {
        tarParser = Mockito.mock(DockerTarParser.class);
        imageInspector = new ImageInspector(tarParser);
    }

    @Test
    void testGetTarExtractionDirectory() {
        File workingDir = new File("src/test/resources/working");
        File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        assertTrue(tarExtractionDirectory.getAbsolutePath().endsWith("src/test/resources/working/tarExtraction"));
    }

    @Test
    void testExtractLayerTars() throws IOException {
        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        imageInspector.extractLayerTars(tarExtractionDirectory, dockerTarfile);
        Mockito.verify(tarParser).unPackImageTar(tarExtractionDirectory, dockerTarfile);
    }

    @Test
    void testGetLayerMapping() throws IntegrationException {
        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        GsonBuilder gsonBuilder = new GsonBuilder();
        String imageRepo = "alpine";
        String imageTag = "latest";
        imageInspector.getLayerMapping(gsonBuilder, tarExtractionDirectory, dockerTarfile.getName(), imageRepo, imageTag);
        Mockito.verify(tarParser).getLayerMapping(gsonBuilder, tarExtractionDirectory, dockerTarfile.getName(), imageRepo, imageTag);
    }

    @Test
    void testCreateInitialImageComponentHierarchy() throws IntegrationException {
        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        String imageRepo = "alpine";
        String imageTag = "latest";

        String imageConfigFileContents = "testConfig";
        List<String> layers = getLayers();
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        imageInspector.createInitialImageComponentHierarchy(tarExtractionDirectory, dockerTarfile.getName(), manifestLayerMapping);
        Mockito.verify(tarParser).createInitialImageComponentHierarchy(tarExtractionDirectory, dockerTarfile.getName(), manifestLayerMapping);
    }

    @Test
    void testExtractDockerLayers() throws IOException, WrongInspectorOsException {
        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
        String imageRepo = "alpine";
        String imageTag = "latest";
        String imageConfigFileContents = "testConfig";
        List<String> layers = getLayers();
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
        List<File> layerTars = new ArrayList<>();
        File layerTar = new File(tarExtractionDirectory, String.format("%s/aaa/layer.tar", dockerTarfile.getName()));
        layerTars.add(layerTar);

        File targetImageFileSystemParentDir = new File(tarExtractionDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        TargetImageFileSystem targetImageFileSystem = new TargetImageFileSystem(targetImageFileSystemRootDir);
        String manifestFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/alpine.tar/manifest.json"), StandardCharsets.UTF_8);
        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy(manifestFileContents, imageConfigFileContents);
        GsonBuilder gsonBuilder = new GsonBuilder();
        imageInspector.extractDockerLayers(gsonBuilder, ImageInspectorOsEnum.ALPINE, null, imageComponentHierarchy, targetImageFileSystem, layerTars, manifestLayerMapping, null);
        Mockito.verify(tarParser).extractImageLayers(gsonBuilder, ImageInspectorOsEnum.ALPINE, null, imageComponentHierarchy, targetImageFileSystem, layerTars, manifestLayerMapping, null);
    }

    @Test
    void testGenerateBdioFromGivenComponentsFull() throws IOException {
        String codeLocationPrefix = "testCodeLocationPrefix";
        String distro = "alpine";
        String tag = "latest";
        String pkgMgrId = "APK";
        ImageInfoDerived imageInfoDerived = doGeneratedBdioFromGivenComponentsTest(codeLocationPrefix, distro, tag, pkgMgrId, false);
        assertEquals(String.format("%s_%s_%s_%s", codeLocationPrefix, distro, tag, pkgMgrId), imageInfoDerived.getCodeLocationName());
    }

    @Test
    void testGenerateBdioFromGivenComponentsApp() throws IOException {
        String codeLocationPrefix = "testCodeLocationPrefix";
        String distro = "alpine";
        String tag = "latest";
        String pkgMgrId = "APK";
        ImageInfoDerived imageInfoDerived = doGeneratedBdioFromGivenComponentsTest(codeLocationPrefix, "alpine", "latest", "APK", true);
        assertEquals(String.format("%s_%s_%s_app_%s", codeLocationPrefix, distro, tag, pkgMgrId), imageInfoDerived.getCodeLocationName());
    }

    private ImageInfoDerived doGeneratedBdioFromGivenComponentsTest(String codeLocationPrefix, String distro, String tag, String pkgMgrId, boolean platformComponentsExcluded) throws IOException {
        TestScenario testScenario = setupTestScenario(codeLocationPrefix, distro, tag, pkgMgrId);

        ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromGivenComponents(TestUtils.createBdioGenerator(),
            testScenario.getImageInfoParsed(),
            testScenario.getImageComponentHierarchy(), testScenario.getManifestLayerMapping(),
            testScenario.getBlackDuckProjectName(), testScenario.getBlackDuckProjectVersion(),
            codeLocationPrefix, true, true, platformComponentsExcluded);

        assertEquals(testScenario.getBlackDuckProjectName(), imageInfoDerived.getFinalProjectName());
        assertEquals(testScenario.getBlackDuckProjectVersion(), imageInfoDerived.getFinalProjectVersionName());
        assertEquals(testScenario.getPkgMgrId(), imageInfoDerived.getImageInfoParsed().getImagePkgMgrDatabase().getPackageManager().name());
        assertEquals(testScenario.getRepo(), imageInfoDerived.getManifestLayerMapping().getImageName());
        assertEquals(testScenario.getLayers().get(0), imageInfoDerived.getManifestLayerMapping().getLayerInternalIds().get(0));
        assertEquals(testScenario.getLayers().get(1), imageInfoDerived.getManifestLayerMapping().getLayerInternalIds().get(1));
        assertEquals(String.format("%s/%s", testScenario.getBlackDuckProjectName(), testScenario.getBlackDuckProjectVersion()),
            imageInfoDerived.getBdioDocument().getProject().bdioExternalIdentifier.externalId);
        return imageInfoDerived;
    }

    private TestScenario setupTestScenario(String codeLocationPrefix, String distro, String tag, String pkgMgrId) throws IOException {
        File workingDir = new File("src/test/resources/working");
        File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        String imageConfigFileContents = "testConfig";
        List<String> layers = getLayers();
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(distro, tag, imageConfigFileContents, layers);

        String manifestFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/alpine.tar/manifest.json"), StandardCharsets.UTF_8);
        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy(manifestFileContents, imageConfigFileContents);
        String blackDuckProjectName = "testProjectName";
        String blackDuckProjectVersion = "testProjectVersion";
        File extractedPackageManagerDirectory = new File("src/test/resources/imageDir/alpine/lib/apk");
        ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(extractedPackageManagerDirectory, PackageManagerEnum.APK);
        PkgMgr pkgMgr = new ApkPkgMgr(new FileOperations());
        ImageInfoParsed imageInfoParsed = new ImageInfoParsed(generateTargetImageFileSystem(tarExtractionDirectory, distro, tag), imagePkgMgrDatabase, distro, pkgMgr);

        return new TestScenario(blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix, distro, tag, pkgMgrId, imageConfigFileContents, layers, manifestLayerMapping, manifestFileContents, imageComponentHierarchy, imageInfoParsed);

    }

    @NotNull
    private List<String> getLayers() {
        List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        return layers;
    }

    @NotNull
    private TargetImageFileSystem generateTargetImageFileSystem(File tarExtractionDirectory, String imageRepo, String imageTag) {
        File targetImageFileSystemParentDir = new File(tarExtractionDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        return new TargetImageFileSystem(targetImageFileSystemRootDir);
    }

    private static class TestScenario {
        private final String blackDuckProjectName;
        private final String blackDuckProjectVersion;
        private final String codeLocationPrefix;
        private final String repo;
        private final String tag;
        private final String pkgMgrId;
        private final String configFileContents;
        private final List<String> layers;
        private final ManifestLayerMapping manifestLayerMapping;
        private final String manifestFileContents;
        private final ImageComponentHierarchy imageComponentHierarchy;
        private final ImageInfoParsed imageInfoParsed;

        public TestScenario(String blackDuckProjectName, String blackDuckProjectVersion, String codeLocationPrefix, final String repo, final String tag, String pkgMgrId, final String configFileContents, final List<String> layers, final ManifestLayerMapping manifestLayerMapping, final String manifestFileContents,
            final ImageComponentHierarchy imageComponentHierarchy, final ImageInfoParsed imageInfoParsed) {
            this.blackDuckProjectName = blackDuckProjectName;
            this.blackDuckProjectVersion = blackDuckProjectVersion;
            this.codeLocationPrefix = codeLocationPrefix;
            this.repo = repo;
            this.tag = tag;
            this.pkgMgrId = pkgMgrId;
            this.configFileContents = configFileContents;
            this.layers = layers;
            this.manifestLayerMapping = manifestLayerMapping;
            this.manifestFileContents = manifestFileContents;
            this.imageComponentHierarchy = imageComponentHierarchy;
            this.imageInfoParsed = imageInfoParsed;
        }

        public String getBlackDuckProjectName() {
            return blackDuckProjectName;
        }

        public String getBlackDuckProjectVersion() {
            return blackDuckProjectVersion;
        }

        public String getCodeLocationPrefix() {
            return codeLocationPrefix;
        }

        public String getRepo() {
            return repo;
        }

        public String getTag() {
            return tag;
        }

        public String getPkgMgrId() {
            return pkgMgrId;
        }

        public String getConfigFileContents() {
            return configFileContents;
        }

        public List<String> getLayers() {
            return layers;
        }

        public ManifestLayerMapping getManifestLayerMapping() {
            return manifestLayerMapping;
        }

        public String getManifestFileContents() {
            return manifestFileContents;
        }

        public ImageComponentHierarchy getImageComponentHierarchy() {
            return imageComponentHierarchy;
        }

        public ImageInfoParsed getImageInfoParsed() {
            return imageInfoParsed;
        }
    }
}
