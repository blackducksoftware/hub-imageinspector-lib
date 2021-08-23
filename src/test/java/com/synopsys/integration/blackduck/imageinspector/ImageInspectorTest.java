package com.synopsys.integration.blackduck.imageinspector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystem;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.linux.TarOperations;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk.ApkPkgMgr;

@Disabled
class ImageInspectorTest {
    private TarOperations tarOperations;
    private ImageInspector imageInspector;

    @BeforeEach
    public void setUpEach() {
        tarOperations = Mockito.mock(TarOperations.class);
        // TODO should some of these be mocked?
        // NEED: Os os, List<PkgMgr> pkgMgrs, PkgMgrExecutor pkgMgrExecutor, CmdExecutor cmdExecutor,
        // TODO revisit testing of this class in general
//        imageInspector = new ImageInspector(tarParser, tarOperations, new Gson(),
//                new FileOperations(), new DockerImageConfigParser(), new DockerManifestFactory());
    }

    @Test
    void testGetTarExtractionDirectory() {
        File workingDir = new File("src/test/resources/working");
        File tarExtractionDirectory = new File(workingDir, WorkingDirectories.TAR_EXTRACTION_DIRECTORY);
        assertTrue(tarExtractionDirectory.getAbsolutePath().endsWith("src/test/resources/working/tarExtraction"));
    }

//    @Test
//    void testExtractImageTar() throws IOException {
//        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
//        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
//        File imageDir = new File(tarExtractionDirectory, dockerTarfile.getName());
//        imageInspector.extractImageTar(imageDir, dockerTarfile);
//        Mockito.verify(tarOperations).extractTarToGivenDir(imageDir, dockerTarfile);
//    }

    // TODO these seems pointless
//    @Test
//    void testGetLayerArchives() throws IOException {
//        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
//        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
//        File extractionDir = new File(tarExtractionDirectory, dockerTarfile.getName());
//        List<TypedArchiveFile> layerTars = imageInspector.getLayerArchives(extractionDir);
//        Mockito.verify(tarParser).getLayerArchives(extractionDir);
//    }
//    @Test
//    void testGetLayerMapping() throws IntegrationException {
//        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
//        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
//        File imageDir = new File(tarExtractionDirectory, dockerTarfile.getName());
//        Gson gson = new Gson();
//        String imageRepo = "alpine";
//        String imageTag = "latest";
//        imageInspector.getLayerMapping(gson, imageDir, imageRepo, imageTag);
//        Mockito.verify(tarParser).getLayerMapping(gson, imageDir, imageRepo, imageTag);
//    }

    // TODO make sure this is tested somewhere
//    @Test
//    void testExtractDockerLayers() throws IOException, WrongInspectorOsException {
//        File tarExtractionDirectory = new File("src/test/resources/working/tarExtraction");
//        File dockerTarfile = new File("src/test/resources/testDockerTarfile");
//        String imageRepo = "alpine";
//        String imageTag = "latest";
//        String imageConfigFileContents = "testConfig";
//        List<String> layers = getLayers();
//        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(imageRepo, imageTag, imageConfigFileContents, layers);
//        FullLayerMapping fullLayerMapping = new FullLayerMapping(manifestLayerMapping, new ArrayList<>(0));
//        List<TypedArchiveFile> layerTars = new ArrayList<>();
//        File layerTar = new File(tarExtractionDirectory, String.format("%s/aaa/layer.tar", dockerTarfile.getName()));
//        layerTars.add(new TypedArchiveFile(ArchiveFileType.TAR, layerTar));
//
//        File targetImageFileSystemParentDir = new File(tarExtractionDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
//        File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
//        ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir);
//        // TODO test componentHierarchyBuilder?
//        PackageGetter packageGetter = Mockito.mock(PackageGetter.class);
//        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
//        imageInspector.extractDockerLayers(ImageInspectorOsEnum.ALPINE, null, containerFileSystem, layerTars, fullLayerMapping, null, componentHierarchyBuilder);
//        Mockito.verify(tarParser).extractPkgMgrDb(ImageInspectorOsEnum.ALPINE, null, containerFileSystem, layerTars, fullLayerMapping, null);
//    }

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
            testScenario.getFullLayerMapping(),
            testScenario.getImageComponentHierarchy(),
            testScenario.getBlackDuckProjectName(), testScenario.getBlackDuckProjectVersion(),
            codeLocationPrefix, true, true, platformComponentsExcluded);

        assertEquals(testScenario.getBlackDuckProjectName(), imageInfoDerived.getFinalProjectName());
        assertEquals(testScenario.getBlackDuckProjectVersion(), imageInfoDerived.getFinalProjectVersionName());
        assertEquals(testScenario.getPkgMgrId(), imageInfoDerived.getImageInfoParsed().getImagePkgMgrDatabase().getPackageManager().name());
        assertEquals(testScenario.getRepo(), imageInfoDerived.getFullLayerMapping().getManifestLayerMapping().getImageName().get());
        assertEquals(testScenario.getLayers().get(0), imageInfoDerived.getFullLayerMapping().getManifestLayerMapping().getLayerInternalIds().get(0));
        assertEquals(testScenario.getLayers().get(1), imageInfoDerived.getFullLayerMapping().getManifestLayerMapping().getLayerInternalIds().get(1));
        assertEquals(String.format("%s/%s", testScenario.getBlackDuckProjectName(), testScenario.getBlackDuckProjectVersion()),
            imageInfoDerived.getBdioDocument().getProject().bdioExternalIdentifier.externalId);
        return imageInfoDerived;
    }

    private TestScenario setupTestScenario(String codeLocationPrefix, String distro, String tag, String pkgMgrId) throws IOException {
        File workingDir = new File("src/test/resources/working");
        File tarExtractionDirectory = new File(workingDir, WorkingDirectories.TAR_EXTRACTION_DIRECTORY);
        String imageConfigFileContents = "testConfig";
        List<String> layers = getLayers();
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(distro, tag, imageConfigFileContents, layers);
        FullLayerMapping fullLayerMapping = new FullLayerMapping(manifestLayerMapping, new ArrayList<>(0));

        String manifestFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/alpine.tar/manifest.json"), StandardCharsets.UTF_8);
        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy();
        String blackDuckProjectName = "testProjectName";
        String blackDuckProjectVersion = "testProjectVersion";
        File extractedPackageManagerDirectory = new File("src/test/resources/imageDir/alpine/lib/apk");
        ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(extractedPackageManagerDirectory, PackageManagerEnum.APK);
        PkgMgr pkgMgr = new ApkPkgMgr(new FileOperations());
        ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = new ContainerFileSystemWithPkgMgrDb(generateTargetImageFileSystem(tarExtractionDirectory, distro, tag), imagePkgMgrDatabase, distro, pkgMgr);

        return new TestScenario(blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix, distro, tag, pkgMgrId, imageConfigFileContents, layers, fullLayerMapping, manifestFileContents, imageComponentHierarchy, containerFileSystemWithPkgMgrDb);

    }

    @NotNull
    private List<String> getLayers() {
        List<String> layers = new ArrayList<>();
        layers.add("testLayer1");
        layers.add("testLayer2");
        return layers;
    }

    @NotNull
    private ContainerFileSystem generateTargetImageFileSystem(File tarExtractionDirectory, String imageRepo, String imageTag) {
        File targetImageFileSystemParentDir = new File(tarExtractionDirectory, WorkingDirectories.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        return new ContainerFileSystem(targetImageFileSystemRootDir);
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
        private final FullLayerMapping fullLayerMapping;
        private final String manifestFileContents;
        private final ImageComponentHierarchy imageComponentHierarchy;
        private final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb;

        public TestScenario(String blackDuckProjectName, String blackDuckProjectVersion, String codeLocationPrefix, final String repo, final String tag, String pkgMgrId, final String configFileContents, final List<String> layers, final FullLayerMapping fullLayerMapping, final String manifestFileContents,
            final ImageComponentHierarchy imageComponentHierarchy, final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb) {
            this.blackDuckProjectName = blackDuckProjectName;
            this.blackDuckProjectVersion = blackDuckProjectVersion;
            this.codeLocationPrefix = codeLocationPrefix;
            this.repo = repo;
            this.tag = tag;
            this.pkgMgrId = pkgMgrId;
            this.configFileContents = configFileContents;
            this.layers = layers;
            this.fullLayerMapping = fullLayerMapping;
            this.manifestFileContents = manifestFileContents;
            this.imageComponentHierarchy = imageComponentHierarchy;
            this.containerFileSystemWithPkgMgrDb = containerFileSystemWithPkgMgrDb;
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

        public FullLayerMapping getFullLayerMapping() {
            return fullLayerMapping;
        }

        public String getManifestFileContents() {
            return manifestFileContents;
        }

        public ImageComponentHierarchy getImageComponentHierarchy() {
            return imageComponentHierarchy;
        }

        public ContainerFileSystemWithPkgMgrDb getImageInfoParsed() {
            return containerFileSystemWithPkgMgrDb;
        }
    }
}
