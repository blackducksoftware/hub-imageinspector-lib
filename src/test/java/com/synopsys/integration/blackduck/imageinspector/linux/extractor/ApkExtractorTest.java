/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.TestUtils;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgr;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ExecutorMock;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioWriter;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

public class ApkExtractorTest {

    @Test
    public void testApkFile1() throws IntegrationException, IOException, InterruptedException {
        testApkExtraction("alpine_apk_output_1.txt", "testApkBdio1.jsonld");
    }

    private void testApkExtraction(final String resourceName, final String bdioOutputFileName) throws IOException, IntegrationException, InterruptedException {
        final File resourceFile = new File(String.format("src/test/resources/%s", resourceName));

        final ApkExtractor extractor = new ApkExtractor();
        final ExecutorMock executor = new ExecutorMock(resourceFile);
        final List<Forge> forges = Arrays.asList(OperatingSystemEnum.ALPINE.getForge());
        extractor.initValues(PackageManagerEnum.APK, executor, forges);

        File bdioOutputFile = new File("test");
        bdioOutputFile = new File(bdioOutputFile, bdioOutputFileName);
        if (bdioOutputFile.exists()) {
            bdioOutputFile.delete();
        }
        bdioOutputFile.getParentFile().mkdirs();
        final BdioWriter bdioWriter = new BdioWriter(new Gson(), new FileWriter(bdioOutputFile));

        final ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File("nonexistentdir"), PackageManagerEnum.APK);
        final SimpleBdioDocument bdioDocument = extractor.extract("root", "1.0", imagePkgMgr, "x86", "CodeLocationName", "Test", "1", null);
        Extractor.writeBdio(bdioWriter, bdioDocument);
        bdioWriter.close();

        final File file1 = new File("src/test/resources/testApkBdio1.jsonld");
        final File file2 = new File("test/testApkBdio1.jsonld");
        System.out.println(String.format("Comparing %s to %s", file2.getAbsolutePath(), file1.getAbsolutePath()));
        final List<String> linesToExclude = Arrays.asList("\"@id\":", "\"externalSystemTypeId\":", "spdx:created", "Tool: ");
        final boolean filesAreEqual = TestUtils.contentEquals(file1, file2, linesToExclude);

        assertTrue(filesAreEqual);
    }
}
