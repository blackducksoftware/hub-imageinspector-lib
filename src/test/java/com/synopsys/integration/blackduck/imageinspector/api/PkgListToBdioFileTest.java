package com.synopsys.integration.blackduck.imageinspector.api;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioReader;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.model.BdioComponent;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

public class PkgListToBdioFileTest {
    private Gson gson = new Gson();

    @Test
    public void testPkgListToBdioFileUbuntu() throws IOException, IntegrationException {
        BdioGeneratorApi api = new BdioGeneratorApi(gson, new ComponentExtractorFactory(), new BdioGenerator(new SimpleBdioFactory()));
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/dpkg/ubuntu_dpkg_output.txt";
        File pkgMgrOutputFile = new File(pkgMgrOutputFilePath);
        String bdioOutputFilePath = "test/output/bdioFromDpkgOutput.jsonld";
        File bdioFile = new File(bdioOutputFilePath);
        FileUtils.deleteQuietly(bdioFile);
        api.pkgListToBdio(PackageManagerEnum.DPKG, "ubuntu", pkgMgrOutputFile.getAbsolutePath(), bdioFile.getAbsolutePath(), "test-blackDuckProjectName", "test-blackDuckProjectVersion",
            "test-codeLocationName");
        System.out.printf("bdioFile: %s\n", bdioFile.getAbsolutePath());
        SimpleBdioDocument bdioDoc = toBdioDocument(bdioFile);
        verifyBdioDocUbuntu(bdioDoc);
    }

    @Test
    public void testPkgListToBdioLinesUbuntu() throws IntegrationException, IOException {
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/dpkg/ubuntu_dpkg_output.txt";
        String linuxDistroName = "ubuntu";
        PackageManagerEnum pmgMgr = PackageManagerEnum.DPKG;

        SimpleBdioDocument bdioDoc = testPkgListToBdioLines(pkgMgrOutputFilePath, linuxDistroName, pmgMgr);
        verifyBdioDocUbuntu(bdioDoc);
    }

    @Test
    public void testPkgListToBdioLinesCentos() throws IntegrationException, IOException {
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/rpm/centos_minus_vim_plus_bacula.txt";
        String linuxDistroName = "centos";
        PackageManagerEnum pmgMgr = PackageManagerEnum.RPM;

        SimpleBdioDocument bdioDoc = testPkgListToBdioLines(pkgMgrOutputFilePath, linuxDistroName, pmgMgr);
        verifyBdioDocCentosMinusVimPlusBacula(bdioDoc);
    }

    @Test
    public void testPkgListToBdioLinesAlpine() throws IntegrationException, IOException {
        String pkgMgrOutputFilePath = "src/test/resources/pkgMgrOutput/apk/alpine_apk_output.txt";
        String linuxDistroName = "alpine";
        PackageManagerEnum pkgMgrType = PackageManagerEnum.APK;

        SimpleBdioDocument bdioDoc = testPkgListToBdioLines(pkgMgrOutputFilePath, linuxDistroName, pkgMgrType);
        verifyBdioDocAlpine(bdioDoc);
    }

    private SimpleBdioDocument testPkgListToBdioLines(final String pkgMgrOutputFilePath, final String linuxDistroName, final PackageManagerEnum pkgMgrType) throws IOException, IntegrationException {
        BdioGeneratorApi api = new BdioGeneratorApi(gson, new ComponentExtractorFactory(), new BdioGenerator(new SimpleBdioFactory()));
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
        assertEquals("test-blackDuckProjectName", bdioDoc.project.name);
        assertEquals("test-blackDuckProjectVersion", bdioDoc.project.version);
        assertEquals("Project", bdioDoc.project.type);
        assertEquals("test-codeLocationName", bdioDoc.billOfMaterials.spdxName);
        assertEquals(665, bdioDoc.components.size());
        assertEquals("cmake-data", bdioDoc.components.get(0).name);
        assertEquals("3.10.2-1ubuntu2", bdioDoc.components.get(0).version);
        assertEquals("cmake-data/3.10.2-1ubuntu2/all", bdioDoc.components.get(0).bdioExternalIdentifier.externalId);
        assertEquals("passwd", bdioDoc.components.get(100).name);
        assertEquals("1:4.5-1ubuntu1", bdioDoc.components.get(100).version);
        assertEquals("passwd/1:4.5-1ubuntu1/amd64", bdioDoc.components.get(100).bdioExternalIdentifier.externalId);
    }

    private void verifyBdioDocCentosMinusVimPlusBacula(final SimpleBdioDocument bdioDoc) {
        assertEquals("test-blackDuckProjectName", bdioDoc.project.name);
        assertEquals("test-blackDuckProjectVersion", bdioDoc.project.version);
        assertEquals("Project", bdioDoc.project.type);
        assertEquals("test-codeLocationName", bdioDoc.billOfMaterials.spdxName);
        assertEquals(189, bdioDoc.components.size());
        assertEquals("systemd-sysv", bdioDoc.components.get(0).name);
        assertEquals("219-57.el7_5.3", bdioDoc.components.get(0).version);
        assertEquals("systemd-sysv/219-57.el7_5.3/x86_64", bdioDoc.components.get(0).bdioExternalIdentifier.externalId);
    }

    private void verifyBdioDocAlpine(final SimpleBdioDocument bdioDoc) {
        assertEquals("test-blackDuckProjectName", bdioDoc.project.name);
        assertEquals("test-blackDuckProjectVersion", bdioDoc.project.version);
        assertEquals("Project", bdioDoc.project.type);
        assertEquals("test-codeLocationName", bdioDoc.billOfMaterials.spdxName);
        assertEquals(95, bdioDoc.components.size());
        assertEquals("pcsc-lite-libs", bdioDoc.components.get(0).name);
        assertEquals("1.8.22-r0", bdioDoc.components.get(0).version);
        assertEquals("pcsc-lite-libs/1.8.22-r0/x86_64", bdioDoc.components.get(0).bdioExternalIdentifier.externalId);
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
