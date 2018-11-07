package com.synopsys.integration.blackduck.imageinspector;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.api.AppConfig;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ApkExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.DpkgExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ApkComponentExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.DpkgComponentExtractor;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AppConfig.class })
public class ImageInspectorTest {

    @MockBean
    private DpkgExecutor dpkgExecutor;

    @MockBean
    private ApkExecutor apkExecutor;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testDpkg() throws IOException, IntegrationException, InterruptedException {
        final List<String> fileLines = FileUtils.readLines(new File("src/test/resources/ubuntu_dpkg_output_1.txt"), StandardCharsets.UTF_8);
        final String[] packages = fileLines.toArray(new String[fileLines.size()]);
        final PkgMgrExecutor executor = dpkgExecutor;
        Mockito.when(executor.runPackageManager(Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(packages);

        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(new File("src/test/resources/imageDir"), PackageManagerEnum.DPKG);
        doTest("ubuntu", "1.0", imagePkgMgrDatabase, new DpkgComponentExtractor(executor));
    }

    @Test
    public void testApk() throws IOException, IntegrationException, InterruptedException {
        final List<String> fileLines = FileUtils.readLines(new File("src/test/resources/alpine_apk_output_1.txt"), StandardCharsets.UTF_8);
        final String[] packages = fileLines.toArray(new String[fileLines.size()]);
        final PkgMgrExecutor executor = apkExecutor;
        Mockito.when(executor.runPackageManager(Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(packages);
        final File imageFileSystem = new File("src/test/resources/imageDir");
        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(imageFileSystem, PackageManagerEnum.APK);
        doTest("alpine", "1.0", imagePkgMgrDatabase, new ApkComponentExtractor(executor, imageFileSystem));
    }

    private void doTest(final String imageName, final String tagName, final ImagePkgMgrDatabase imagePkgMgrDatabase, final ComponentExtractor componentExtractor)
            throws FileNotFoundException, IOException, IntegrationException, InterruptedException {

        final PackageManagerEnum pkgMgr = imagePkgMgrDatabase.getPackageManager();
        final File imageFilesDir = new File("src/test/resources/imageDir");
        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final BdioGenerator bdioGenerator = new BdioGenerator(simpleBdioFactory);

        final ImageInfoParsed imageInfoParsed = new ImageInfoParsed(new File(String.format("image_%s_v_%s", imageName, tagName)), imagePkgMgrDatabase, imageName);

        final ImageInspector imageInspector = new ImageInspector();

        final String tempDirPath = TestUtils.createTempDirectory().getAbsolutePath();

        final DockerTarParser tarParser = Mockito.mock(DockerTarParser.class);
        Mockito.when(tarParser.parseImageInfo(Mockito.any(File.class))).thenReturn(imageInfoParsed);
        ComponentExtractorFactory componentExtractorFactory = Mockito.mock(ComponentExtractorFactory.class);
        Mockito.when(componentExtractorFactory.createComponentExtractor(Mockito.any(Gson.class), Mockito.any(File.class), Mockito.any(PackageManagerEnum.class))).thenReturn(componentExtractor);
        imageInspector.setTarParser(tarParser);
        imageInspector.setComponentExtractorFactory(componentExtractorFactory);
        final List<ManifestLayerMapping> mappings = new ArrayList<>();
        final List<String> layerIds = new ArrayList<>();
        layerIds.add("testLayerId");
        final ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layerIds);
        mappings.add(mapping);

        final ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(bdioGenerator, imageInfoParsed, imageName, tagName, mapping, "testProjectName", "testProjectVersion", imageFilesDir, "");
        final File bdioFile = imageInspector.writeBdioFile(new File(tempDirPath), imageInfoDerived);
        final File file1 = new File(String.format("src/test/resources/%s_imageDir_testProjectName_testProjectVersion_bdio.jsonld", imageName));
        final File file2 = bdioFile;
        System.out.println(String.format("Comparing %s %s", file2.getAbsolutePath(), file1.getAbsolutePath()));
        final List<String> linesToExclude = Arrays.asList("\"@id\":", "\"externalSystemTypeId\":", "_Users_", "spdx:created", "Tool: ");
        final boolean filesAreEqual = TestUtils.contentEquals(file1, file2, linesToExclude);
        assertTrue(filesAreEqual);
    }
}
