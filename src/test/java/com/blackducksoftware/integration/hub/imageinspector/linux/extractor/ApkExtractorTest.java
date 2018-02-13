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
package com.blackducksoftware.integration.hub.imageinspector.linux.extractor;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.blackducksoftware.integration.hub.bdio.BdioWriter;
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.TestUtils;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.ImagePkgMgr;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.lib.PackageManagerEnum;
import com.blackducksoftware.integration.hub.imageinspector.linux.executor.ExecutorMock;
import com.google.gson.Gson;

public class ApkExtractorTest {

    @Test
    public void testApkFile1() throws HubIntegrationException, IOException, InterruptedException {
        testApkExtraction("alpine_apk_output_1.txt", "testApkBdio1.jsonld");
    }

    private void testApkExtraction(final String resourceName, final String bdioOutputFileName) throws IOException, HubIntegrationException, InterruptedException {
        final File resourceFile = new File(String.format("src/test/resources/%s", resourceName));

        final ApkExtractor extractor = new ApkExtractor();
        final ExecutorMock executor = new ExecutorMock(resourceFile);
        final List<String> forges = Arrays.asList(OperatingSystemEnum.ALPINE.getForge());
        extractor.initValues(PackageManagerEnum.APK, executor, forges);

        File bdioOutputFile = new File("test");
        bdioOutputFile = new File(bdioOutputFile, bdioOutputFileName);
        if (bdioOutputFile.exists()) {
            bdioOutputFile.delete();
        }
        bdioOutputFile.getParentFile().mkdirs();
        final BdioWriter bdioWriter = new BdioWriter(new Gson(), new FileWriter(bdioOutputFile));

        final ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File("nonexistentdir"), PackageManagerEnum.APK);
        final SimpleBdioDocument bdioDocument = extractor.extract("root", "1.0", imagePkgMgr, "x86", "CodeLocationName", "Test", "1");
        Extractor.writeBdio(bdioWriter, bdioDocument);
        bdioWriter.close();

        final File file1 = new File("src/test/resources/testApkBdio1.jsonld");
        final File file2 = new File("test/testApkBdio1.jsonld");
        System.out.println(String.format("Comparing %s to %s", file2.getAbsolutePath(), file1.getAbsolutePath()));
        final List<String> ignoreLinesWith = Arrays.asList("\"@id\":", "\"externalSystemTypeId\":");
        final boolean filesAreEqual = TestUtils.contentEquals(file1, file2, ignoreLinesWith);

        assertTrue(filesAreEqual);
    }
}
