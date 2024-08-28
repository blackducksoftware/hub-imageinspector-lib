package com.blackduck.integration.blackduck.imageinspector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.blackduck.integration.blackduck.imageinspector.api.name.Names;
import com.blackduck.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystem;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemCompatibilityChecker;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.PkgMgrDbExtractor;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchyLogger;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.blackduck.integration.blackduck.imageinspector.image.common.*;
import com.synopsys.integration.blackduck.imageinspector.image.common.*;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ImageComponentHierarchy;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.ImageLayerArchiveExtractor;
import com.blackduck.integration.blackduck.imageinspector.linux.Os;
import com.blackduck.integration.blackduck.imageinspector.linux.TarOperations;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk.ApkPkgMgr;

class ImageInspectorTest {
    private TarOperations tarOperations;
    private ImageInspector imageInspector;

    @BeforeEach
    public void setUpEach() {
        tarOperations = Mockito.mock(TarOperations.class);
        Os os = Mockito.mock(Os.class);
        PkgMgrDbExtractor pkgMgrDbExtractor = Mockito.mock(PkgMgrDbExtractor.class);
        ImageLayerArchiveExtractor imageLayerArchiveExtractor = new ImageLayerArchiveExtractor();
        FileOperations fileOperations = new FileOperations();
        ContainerFileSystemCompatibilityChecker containerFileSystemCompatibilityChecker = new ContainerFileSystemCompatibilityChecker();
        ImageLayerApplier imageLayerApplier = new ImageLayerApplier(fileOperations, imageLayerArchiveExtractor);
        BdioGenerator bdioGenerator = new BdioGenerator();
        ImageDirectoryDataExtractorFactoryChooser imageDirectoryDataExtractorFactoryChooser = new ImageDirectoryDataExtractorFactoryChooser();
        ImageComponentHierarchyLogger imageComponentHierarchyLogger = new ImageComponentHierarchyLogger();
        imageInspector = new ImageInspector(os, pkgMgrDbExtractor, tarOperations,
                fileOperations, imageLayerApplier, containerFileSystemCompatibilityChecker,
                bdioGenerator, imageDirectoryDataExtractorFactoryChooser,
                imageComponentHierarchyLogger);
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
            testScenario.getFullLayerMapping(),
            testScenario.getImageComponentHierarchy(),
            testScenario.getBlackDuckProjectName(), testScenario.getBlackDuckProjectVersion(),
            "testImage.tar",
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
        File tarExtractionDirectory = new File(workingDir, "tarExtraction");
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
        File targetImageFileSystemParentDir = new File(tarExtractionDirectory, "imageFiles");
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
