package com.synopsys.integration.blackduck.imageinspector.api;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.bdio.BdioReader;
import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;

public class BdioGeneratorApiFuncTest {
    private Gson gson = new Gson();

    @Test
    public void testPkgListToBdioFileUbuntu() throws IOException, IntegrationException {
        BdioGeneratorApi api = new BdioGeneratorApi(gson, new PkgMgrFactory(), new BdioGenerator(new SimpleBdioFactory()));
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/dpkg/ubuntu_dpkg_output.txt";
        File pkgMgrOutputFile = new File(pkgMgrOutputFilePath);
        String bdioOutputFilePath = "test/output/bdioFromDpkgOutput.jsonld";
        File bdioFile = new File(bdioOutputFilePath);
        FileUtils.deleteQuietly(bdioFile);
        api.pkgListToBdio(PackageManagerEnum.DPKG, BdioGeneratorApi.LINUX_DISTRO_NAME_UBUNTU, pkgMgrOutputFile.getAbsolutePath(), bdioFile.getAbsolutePath(), "test-blackDuckProjectName", "test-blackDuckProjectVersion",
            "test-codeLocationName");
        System.out.printf("bdioFile: %s\n", bdioFile.getAbsolutePath());
        SimpleBdioDocument bdioDoc = toBdioDocument(bdioFile);
        verifyBdioDocUbuntu(bdioDoc);
    }

    @Test
    public void testPkgListToBdioLinesUbuntu() throws IntegrationException, IOException {
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/dpkg/ubuntu_dpkg_output.txt";
        SimpleBdioDocument bdioDoc = testPkgListToBdioLines(pkgMgrOutputFilePath, BdioGeneratorApi.LINUX_DISTRO_NAME_UBUNTU, PackageManagerEnum.DPKG);
        verifyBdioDocUbuntu(bdioDoc);
    }

    @Test
    public void testPkgListToBdioLinesCentos() throws IntegrationException, IOException {
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/rpm/centos_minus_vim_plus_bacula.txt";
        SimpleBdioDocument bdioDoc = testPkgListToBdioLines(pkgMgrOutputFilePath, BdioGeneratorApi.LINUX_DISTRO_NAME_CENTOS, PackageManagerEnum.RPM);
        verifyBdioDocCentosMinusVimPlusBacula(bdioDoc);
    }

    @Test
    public void testPkgListToBdioFileAlpine() throws IOException, IntegrationException {
        BdioGeneratorApi api = new BdioGeneratorApi(gson, new PkgMgrFactory(), new BdioGenerator(new SimpleBdioFactory()));
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/apk/alpine_apk_output.txt";
        File pkgMgrOutputFile = new File(pkgMgrOutputFilePath);
        String bdioOutputFilePath = "test/output/bdioFromApkOutput.jsonld";
        File bdioFile = new File(bdioOutputFilePath);
        FileUtils.deleteQuietly(bdioFile);
        api.pkgListToBdioApk("x86_64", BdioGeneratorApi.LINUX_DISTRO_NAME_ALPINE, pkgMgrOutputFile.getAbsolutePath(), bdioFile.getAbsolutePath(), "test-blackDuckProjectName", "test-blackDuckProjectVersion",
            "test-codeLocationName");
        System.out.printf("bdioFile: %s\n", bdioFile.getAbsolutePath());
        SimpleBdioDocument bdioDoc = toBdioDocument(bdioFile);
        verifyBdioDocAlpine(bdioDoc);
    }

    @Test
    public void testPkgListToBdioLinesAlpine() throws IntegrationException, IOException {
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/apk/alpine_apk_output.txt";
        SimpleBdioDocument bdioDoc = testPkgListToBdioLines(pkgMgrOutputFilePath, BdioGeneratorApi.LINUX_DISTRO_NAME_ALPINE, PackageManagerEnum.APK);
        verifyBdioDocAlpine(bdioDoc);
    }

    @Test
    public void testPkgListToBdioLinesAlpineUsingNonApkMethod() throws IntegrationException, IOException {
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/apk/alpine_apk_output.txt";
        BdioGeneratorApi api = new BdioGeneratorApi(gson, new PkgMgrFactory(), new BdioGenerator(new SimpleBdioFactory()));
        File pkgMgrOutputFile = new File(pkgMgrOutputFilePath);
        List<String> pkgMgrOutputLinesList = FileUtils.readLines(pkgMgrOutputFile, StandardCharsets.UTF_8);
        String[] pkgMgrOutputLines = pkgMgrOutputLinesList.toArray(new String[pkgMgrOutputLinesList.size()]);
        try {
            api.pkgListToBdio(PackageManagerEnum.APK, BdioGeneratorApi.LINUX_DISTRO_NAME_ALPINE, pkgMgrOutputLines, "test-blackDuckProjectName", "test-blackDuckProjectVersion",
                "test-codeLocationName");
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    private SimpleBdioDocument testPkgListToBdioLines(final String pkgMgrOutputFilePath, final String linuxDistroName, final PackageManagerEnum pkgMgrType) throws IOException, IntegrationException {
        BdioGeneratorApi api = new BdioGeneratorApi(gson, new PkgMgrFactory(), new BdioGenerator(new SimpleBdioFactory()));
        File pkgMgrOutputFile = new File(pkgMgrOutputFilePath);
        List<String> pkgMgrOutputLinesList = FileUtils.readLines(pkgMgrOutputFile, StandardCharsets.UTF_8);
        String[] pkgMgrOutputLines = pkgMgrOutputLinesList.toArray(new String[pkgMgrOutputLinesList.size()]);
        String[] bdioLines;
        if (pkgMgrType == PackageManagerEnum.APK) {
            bdioLines = api.pkgListToBdioApk("x86_64", linuxDistroName, pkgMgrOutputLines, "test-blackDuckProjectName", "test-blackDuckProjectVersion",
                "test-codeLocationName");
        } else {
            bdioLines = api.pkgListToBdio(pkgMgrType, linuxDistroName, pkgMgrOutputLines, "test-blackDuckProjectName", "test-blackDuckProjectVersion",
                "test-codeLocationName");
        }
        SimpleBdioDocument bdioDoc = toBdioDocument(bdioLines);
        return bdioDoc;
    }

    private void verifyBdioDocUbuntu(final SimpleBdioDocument bdioDoc) {
        assertEquals("test-blackDuckProjectName", bdioDoc.getProject().name);
        assertEquals("test-blackDuckProjectVersion", bdioDoc.getProject().version);
        assertEquals("Project", bdioDoc.getProject().type);
        assertEquals("test-codeLocationName", bdioDoc.getBillOfMaterials().spdxName);
        assertEquals(665, bdioDoc.getComponents().size());
        verifyContainsComp(bdioDoc, "libboost-fiber1.65.1", "1.65.1+dfsg-0ubuntu5", "libboost-fiber1.65.1/1.65.1+dfsg-0ubuntu5/amd64");
        verifyContainsComp(bdioDoc, "jblas", "1.2.4-1", "jblas/1.2.4-1/amd64");
    }

    private void verifyBdioDocCentosMinusVimPlusBacula(final SimpleBdioDocument bdioDoc) {
        assertEquals("test-blackDuckProjectName", bdioDoc.getProject().name);
        assertEquals("test-blackDuckProjectVersion", bdioDoc.getProject().version);
        assertEquals("Project", bdioDoc.getProject().type);
        assertEquals("test-codeLocationName", bdioDoc.getBillOfMaterials().spdxName);
        assertEquals(189, bdioDoc.getComponents().size());
        verifyContainsComp(bdioDoc, "perl-Sys-MemInfo", "0.91-7.el7", "perl-Sys-MemInfo/0.91-7.el7/x86_64");
    }

    private void verifyBdioDocAlpine(final SimpleBdioDocument bdioDoc) {
        assertEquals("test-blackDuckProjectName", bdioDoc.getProject().name);
        assertEquals("test-blackDuckProjectVersion", bdioDoc.getProject().version);
        assertEquals("Project", bdioDoc.getProject().type);
        assertEquals("test-codeLocationName", bdioDoc.getBillOfMaterials().spdxName);
        assertEquals(95, bdioDoc.getComponents().size());
        verifyContainsComp(bdioDoc, "boost-python", "1.62.0-r5", "boost-python/1.62.0-r5/x86_64");
    }

    private void verifyContainsComp(final SimpleBdioDocument bdioDoc, final String compName, final String compVersion, final String compExternalId) {
        boolean foundComp = false;
        for (BdioComponent comp : bdioDoc.getComponents()) {
            if (comp.name.equals(compName)) {
                foundComp = true;
                assertEquals(compVersion, comp.version);
                assertEquals(compExternalId, comp.bdioExternalIdentifier.externalId);
            }
        }
        assertTrue(foundComp);
    }

    private SimpleBdioDocument toBdioDocument(final String[] bdioLines) throws IOException {
        final Reader reader = new StringReader(String.join("\n", bdioLines));
        return generateSimpleBdioDocument(reader);
    }

    private SimpleBdioDocument toBdioDocument(final File bdioFile) throws IOException {
        final Reader reader = new FileReader(bdioFile);
        return generateSimpleBdioDocument(reader);
    }

    private SimpleBdioDocument generateSimpleBdioDocument(final Reader reader) throws IOException {
        SimpleBdioDocument doc = null;
        try (BdioReader bdioReader = new BdioReader(gson, reader)) {
            doc = bdioReader.readSimpleBdioDocument();
            return doc;
        }
    }
}
