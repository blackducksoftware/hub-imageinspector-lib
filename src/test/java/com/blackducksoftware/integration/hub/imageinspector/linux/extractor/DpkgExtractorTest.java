package com.blackducksoftware.integration.hub.imageinspector.linux.extractor;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.bdio.BdioWriter;
import com.blackducksoftware.integration.hub.bdio.graph.MutableDependencyGraph;
import com.blackducksoftware.integration.hub.bdio.graph.MutableMapDependencyGraph;
import com.blackducksoftware.integration.hub.bdio.model.Forge;
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency;
import com.blackducksoftware.integration.hub.imageinspector.TestUtils;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.ImagePkgMgr;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.lib.PackageManagerEnum;
import com.blackducksoftware.integration.hub.imageinspector.linux.executor.ExecutorMock;
import com.google.gson.Gson;

public class DpkgExtractorTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testCreateBdioComponentWithoutPreferred() {
        final DpkgExtractor extractor = new DpkgExtractor();
        extractor.init();
        final MutableDependencyGraph dependencies = new MutableMapDependencyGraph();
        extractor.createBdioComponent(dependencies, "testName", "testVersion", "testExternalId", "testArch", null);
        for (final Dependency d : dependencies.getRootDependencies()) {
            System.out.printf("dependency name: %s\n", d.name);
            System.out.printf("dependency version: %s\n", d.version);
            System.out.printf("dependency externalId arch: %s\n", d.externalId.architecture);
            System.out.printf("dependency externalId forge name: %s\n", d.externalId.forge.getName());
            System.out.printf("dependency externalId forge separator: %s\n", d.externalId.forge.getSeparator());
            System.out.printf("dependency externalId forge kBSeparator: %s\n", d.externalId.forge.getKbSeparator());
        }
    }

    @Test
    public void testCreateBdioComponentWithPreferred() {
        final DpkgExtractor extractor = new DpkgExtractor();
        extractor.init();
        final MutableDependencyGraph dependencies = new MutableMapDependencyGraph();
        extractor.createBdioComponent(dependencies, "testName", "testVersion", "testExternalId", "testArch", OperatingSystemEnum.UBUNTU);
        for (final Dependency d : dependencies.getRootDependencies()) {
            System.out.printf("dependency name: %s\n", d.name);
            System.out.printf("dependency version: %s\n", d.version);
            System.out.printf("dependency externalId arch: %s\n", d.externalId.architecture);
            System.out.printf("dependency externalId forge name: %s\n", d.externalId.forge.getName());
            System.out.printf("dependency externalId forge separator: %s\n", d.externalId.forge.getSeparator());
            System.out.printf("dependency externalId forge kBSeparator: %s\n", d.externalId.forge.getKbSeparator());
        }
    }

    @Test
    public void testTemp() {
        final String s = "@ubuntu";
        final String escaped = s.replaceAll("[^A-Za-z0-9]", "_");
        System.out.printf("escaped: %s\n", escaped);
    }

    @Test
    public void testDpkgFile1() throws IntegrationException, IOException, InterruptedException {
        testDpkgExtraction("ubuntu_dpkg_output_1.txt", "testDpkgBdio1.jsonld");
    }

    private void testDpkgExtraction(final String resourceName, final String bdioOutputFileName) throws IOException, IntegrationException, InterruptedException {
        final File resourceFile = new File(String.format("src/test/resources/%s", resourceName));

        final DpkgExtractor extractor = new DpkgExtractor();
        final ExecutorMock executor = new ExecutorMock(resourceFile);
        final List<Forge> forges = Arrays.asList(OperatingSystemEnum.UBUNTU.getForge());
        extractor.initValues(PackageManagerEnum.DPKG, executor, forges);

        File bdioOutputFile = new File("test");
        bdioOutputFile = new File(bdioOutputFile, bdioOutputFileName);
        if (bdioOutputFile.exists()) {
            bdioOutputFile.delete();
        }
        bdioOutputFile.getParentFile().mkdirs();
        final BdioWriter bdioWriter = new BdioWriter(new Gson(), new FileWriter(bdioOutputFile));

        final ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File("nonexistentdir"), PackageManagerEnum.DPKG);
        final SimpleBdioDocument bdioDocument = extractor.extract("root", "1.0", imagePkgMgr, "x86", "CodeLocationName", "Test", "1", null);
        Extractor.writeBdio(bdioWriter, bdioDocument);
        bdioWriter.close();

        final File file1 = new File("src/test/resources/testDpkgBdio1.jsonld");
        final File file2 = new File("test/testDpkgBdio1.jsonld");
        System.out.println(String.format("Comparing %s to %s", file2.getAbsolutePath(), file1.getAbsolutePath()));
        final List<String> linesToExclude = Arrays.asList("\"@id\":", "\"externalSystemTypeId\":", "spdx:created");
        final boolean filesAreEqual = TestUtils.contentEquals(file1, file2, linesToExclude);
        assertTrue(filesAreEqual);
    }
}
