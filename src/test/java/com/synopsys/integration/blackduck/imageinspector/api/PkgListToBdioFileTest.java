package com.synopsys.integration.blackduck.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.synopsys.integration.exception.IntegrationException;

public class PkgListToBdioFileTest {

    @Test
    public void testPkgListToBdioFile() throws IOException, IntegrationException {
        ImageInspectorApi api = new ImageInspectorApi();
        String[] pkgMgrListCmdOutputLines = FileUtils.readFileToString(new File("src/test/resources/pkgMgrOutput/dpkg/ubuntu_dpkg_output.txt"), StandardCharsets.UTF_8).split("\n");
        api.pkgListToBdioFile(PackageManagerEnum.DPKG, "ubuntu", pkgMgrListCmdOutputLines, "test/output/bdioFromDpkgOutput.jsonld", "test-blackDuckProjectName", "test-blackDuckProjectVersion",
            "test-codeLocationPrefix",
            true);
    }
}
