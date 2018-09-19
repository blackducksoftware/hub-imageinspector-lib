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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ApkExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.DpkgExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.Extractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ExtractorManager;
import com.synopsys.integration.exception.IntegrationException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AppConfig.class })
public class ImageInspectorTest {

    @Autowired
    private ApkExtractor apkExtractor;

    @Autowired
    private DpkgExtractor dpkgExtractor;

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
        doTest("ubuntu", "1.0", PackageManagerEnum.DPKG, dpkgExtractor, executor);
    }

    @Test
    public void testApk() throws IOException, IntegrationException, InterruptedException {
        final List<String> fileLines = FileUtils.readLines(new File("src/test/resources/alpine_apk_output_1.txt"), StandardCharsets.UTF_8);
        final String[] packages = fileLines.toArray(new String[fileLines.size()]);
        final PkgMgrExecutor executor = apkExecutor;
        Mockito.when(executor.runPackageManager(Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(packages);
        doTest("alpine", "1.0", PackageManagerEnum.APK, apkExtractor, executor);
    }

    private void doTest(final String imageName, final String tagName, final PackageManagerEnum pkgMgr, final Extractor extractor, final PkgMgrExecutor executor)
            throws FileNotFoundException, IOException, IntegrationException, InterruptedException {

        final File imageTarFile = new File("test/image.tar");
        final ImagePkgMgrDatabase imagePkgMgr = new ImagePkgMgrDatabase(new File(String.format("test/resources/imageDir/image_%s_v_%s/%s", imageName, tagName, pkgMgr.getDirectory())), pkgMgr);
        final ImageInfoParsed imageInfo = new ImageInfoParsed(String.format("image_%s_v_%s", imageName, tagName), imagePkgMgr, null);

        final List<Extractor> extractors = new ArrayList<>();
        extractors.add(extractor);
        final ExtractorManager extractorManager = Mockito.mock(ExtractorManager.class);
        Mockito.when(extractorManager.getExtractors()).thenReturn(extractors);
        final ImageInspector imageInspector = new ImageInspector();
        final String tempDirPath = TestUtils.createTempDirectory().getAbsolutePath();
        imageInspector.setExtractorManager(extractorManager);

        final List<File> etcDirs = new ArrayList<>();
        final File etcDir = TestUtils.createTempDirectory();
        final File etcApkDir = new File(etcDir, "apk");
        final File etcApkArchFile = new File(etcApkDir, "arch");
        etcApkDir.mkdirs();
        etcApkArchFile.createNewFile();
        FileUtils.write(etcApkArchFile, "amd64", StandardCharsets.UTF_8);

        etcDirs.add(etcDir);

        final DockerTarParser tarParser = Mockito.mock(DockerTarParser.class);
        Mockito.when(tarParser.collectPkgMgrInfo(Mockito.any(File.class))).thenReturn(imageInfo);
        imageInspector.setTarParser(tarParser);
        final List<ManifestLayerMapping> mappings = new ArrayList<>();
        final List<String> layerIds = new ArrayList<>();
        layerIds.add("testLayerId");
        final ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layerIds);
        mappings.add(mapping);
        final File imageFilesDir = new File("src/test/resources/imageDir");
        final ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(imageName, tagName, mappings, "testProjectName", "testProjectVersion", imageTarFile, imageFilesDir, "", false);
        final File bdioFile = imageInspector.writeBdioFile(new File(tempDirPath), imageInfoDerived);
        final File file1 = new File(String.format("src/test/resources/%s_imageDir_testProjectName_testProjectVersion_bdio.jsonld", imageName));
        final File file2 = bdioFile;
        System.out.println(String.format("Comparing %s to %s", file2.getAbsolutePath(), file1.getAbsolutePath()));
        final List<String> linesToExclude = Arrays.asList("\"@id\":", "\"externalSystemTypeId\":", "_Users_", "spdx:created", "Tool: ");
        final boolean filesAreEqual = TestUtils.contentEquals(file1, file2, linesToExclude);
        assertTrue(filesAreEqual);
    }
}
