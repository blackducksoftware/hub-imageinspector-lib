package com.blackducksoftware.integration.hub.imageinspector;

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
import org.mockito.Mockito;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.bdio.model.Forge;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.DockerTarParser;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.ImageInfoParsed;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.ImagePkgMgr;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInfoDerived;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInspector;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.lib.PackageManagerEnum;
import com.blackducksoftware.integration.hub.imageinspector.linux.executor.ApkExecutor;
import com.blackducksoftware.integration.hub.imageinspector.linux.executor.DpkgExecutor;
import com.blackducksoftware.integration.hub.imageinspector.linux.executor.PkgMgrExecutor;
import com.blackducksoftware.integration.hub.imageinspector.linux.extractor.ApkExtractor;
import com.blackducksoftware.integration.hub.imageinspector.linux.extractor.DpkgExtractor;
import com.blackducksoftware.integration.hub.imageinspector.linux.extractor.Extractor;
import com.blackducksoftware.integration.hub.imageinspector.linux.extractor.ExtractorManager;

public class ImageInspectorTest {

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
        final PkgMgrExecutor executor = Mockito.mock(DpkgExecutor.class);
        Mockito.when(executor.runPackageManager(Mockito.any(ImagePkgMgr.class))).thenReturn(packages);
        final List<Forge> forges = Arrays.asList(OperatingSystemEnum.DEBIAN.getForge(), OperatingSystemEnum.UBUNTU.getForge());
        doTest("ubuntu", "1.0", OperatingSystemEnum.UBUNTU, PackageManagerEnum.DPKG, new DpkgExtractor(), executor, forges);
    }

    @Test
    public void testApk() throws IOException, IntegrationException, InterruptedException {
        final List<String> fileLines = FileUtils.readLines(new File("src/test/resources/alpine_apk_output_1.txt"), StandardCharsets.UTF_8);
        final String[] packages = fileLines.toArray(new String[fileLines.size()]);
        final PkgMgrExecutor executor = Mockito.mock(ApkExecutor.class);
        Mockito.when(executor.runPackageManager(Mockito.any(ImagePkgMgr.class))).thenReturn(packages);
        final List<Forge> forges = Arrays.asList(OperatingSystemEnum.ALPINE.getForge());
        doTest("alpine", "1.0", OperatingSystemEnum.ALPINE, PackageManagerEnum.APK, new ApkExtractor(), executor, forges);
    }

    private void doTest(final String imageName, final String tagName, final OperatingSystemEnum os, final PackageManagerEnum pkgMgr, final Extractor extractor, final PkgMgrExecutor executor, final List<Forge> forges)
            throws FileNotFoundException, IOException, IntegrationException, InterruptedException {

        final File imageTarFile = new File("test/image.tar");
        final ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File(String.format("test/resources/imageDir/image_%s_v_%s/%s", imageName, tagName, pkgMgr.getDirectory())), pkgMgr);
        final ImageInfoParsed imageInfo = new ImageInfoParsed(String.format("image_%s_v_%s", imageName, tagName), os, imagePkgMgr);

        final List<Extractor> extractors = new ArrayList<>();
        extractor.initValues(pkgMgr, executor, forges);
        executor.init();

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
        Mockito.when(tarParser.collectPkgMgrInfo(Mockito.any(File.class), Mockito.any(OperatingSystemEnum.class))).thenReturn(imageInfo);
        imageInspector.setTarParser(tarParser);
        final List<ManifestLayerMapping> mappings = new ArrayList<>();
        final List<String> layerIds = new ArrayList<>();
        layerIds.add("testLayerId");
        final ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layerIds);
        mappings.add(mapping);
        final File imageFilesDir = new File("src/test/resources/imageDir");
        final ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(imageName, tagName, mappings, "testProjectName", "testProjectVersion", imageTarFile, imageFilesDir, os, "");
        final File bdioFile = imageInspector.writeBdioFile(new File(tempDirPath), imageInfoDerived);
        final File file1 = new File(String.format("src/test/resources/%s_imageDir_testProjectName_testProjectVersion_bdio.jsonld", imageName));
        final File file2 = bdioFile;
        System.out.println(String.format("Comparing %s to %s", file2.getAbsolutePath(), file1.getAbsolutePath()));
        final List<String> linesToExclude = Arrays.asList("\"@id\":", "\"externalSystemTypeId\":", "_Users_", "spdx:created");
        final boolean filesAreEqual = TestUtils.contentEquals(file1, file2, linesToExclude);
        assertTrue(filesAreEqual);
    }
}
