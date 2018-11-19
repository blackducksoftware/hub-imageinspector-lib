package com.synopsys.integration.blackduck.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

@Component
public class BdioGeneratorApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ComponentExtractorFactory componentExtractorFactory;
    private Gson gson;
    private final BdioGenerator bdioGenerator;

    public BdioGeneratorApi(final Gson gson, final ComponentExtractorFactory componentExtractorFactory, final BdioGenerator bdioGenerator) {
        this.gson = gson;
        this.componentExtractorFactory = componentExtractorFactory;
        this.bdioGenerator = bdioGenerator;
    }

    public void pkgListToBdio(final PackageManagerEnum pkgMgrType, String linuxDistroName, final String pkgMgrListCmdOutputPath, final String bdioOutputPath, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName) throws IntegrationException {
        File pkgMgrListCmdOutputFile = new File(pkgMgrListCmdOutputPath);
        List<String> pkgMgrListCmdOutputLinesList;
        try {
            pkgMgrListCmdOutputLinesList = FileUtils.readLines(pkgMgrListCmdOutputFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error reading package manager list command output file %s", pkgMgrListCmdOutputFile.getAbsolutePath()), e);
        }
        String[] pkgMgrListCmdOutputLines = pkgMgrListCmdOutputLinesList.toArray(new String[pkgMgrListCmdOutputLinesList.size()]);
        String[] bdioLines = pkgListToBdio(pkgMgrType,  linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName);
        File bdioOutputFile = new File(bdioOutputPath);
        try {
            FileUtils.writeLines(bdioOutputFile, Arrays.asList(bdioLines));
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error writing BDIO file %s", bdioOutputFile.getAbsolutePath()), e);
        }
    }

    public String[] pkgListToBdio(final PackageManagerEnum pkgMgrType, String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName) throws IntegrationException {
        logger.info(String.format("pkgListToBdio(): pkgMgrType: %s; linuxDistroName: %s; pkgMgrListCmdOutputLines: %s, blackDuckProjectName: %s; blackDuckProjectVersion: %s; codeLocationName: %s",
            pkgMgrType, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName));

        if (pkgMgrType != PackageManagerEnum.DPKG && pkgMgrType != PackageManagerEnum.RPM) {
            throw new UnsupportedOperationException("The pkgListToBdioFile() currently only supports DPKG");
        }
        ComponentExtractor extractor = componentExtractorFactory.createComponentExtractor(gson, null, pkgMgrType);
        List<ComponentDetails> comps = extractor.extractComponentsFromPkgMgrOutput(linuxDistroName, pkgMgrListCmdOutputLines);
        logger.info(String.format("Extracted %d components from given package manager output", comps.size()));
        SimpleBdioDocument bdioDoc = bdioGenerator.generateBdioDocument(codeLocationName, blackDuckProjectName, blackDuckProjectVersion, linuxDistroName, comps);
        try {
            return bdioGenerator.getBdioAsStringArray(bdioDoc);
        } catch (IOException e) {
            throw new IntegrationException("Error converting BDIO document to string array", e);
        }
    }
}
