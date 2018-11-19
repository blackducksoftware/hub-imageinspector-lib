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
        pkgListToBdioWithArch(pkgMgrType, linuxDistroName, pkgMgrListCmdOutputPath, bdioOutputPath, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, null);
    }

    public String[] pkgListToBdio(final PackageManagerEnum pkgMgrType, String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName) throws IntegrationException {
        logger.info(String.format("pkgListToBdio(): pkgMgrType: %s; linuxDistroName: %s; pkgMgrListCmdOutputLines: %s, blackDuckProjectName: %s; blackDuckProjectVersion: %s; codeLocationName: %s",
            pkgMgrType, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName));

        return pkgListToBdioWithArch(pkgMgrType, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, null);
    }

    public void pkgListToBdioApk(final String architecture, String linuxDistroName, final String pkgMgrListCmdOutputPath, final String bdioOutputPath, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName) throws IntegrationException {
        pkgListToBdioWithArch(PackageManagerEnum.APK, linuxDistroName, pkgMgrListCmdOutputPath, bdioOutputPath, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, architecture);
    }

    public String[] pkgListToBdioApk(final String architecture, String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName) throws IntegrationException {
        logger.info(String.format("pkgListToBdioApk(): architecture: %s; linuxDistroName: %s; pkgMgrListCmdOutputLines: %s, blackDuckProjectName: %s; blackDuckProjectVersion: %s; codeLocationName: %s",
            architecture, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName));

        return pkgListToBdioWithArch(PackageManagerEnum.APK, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, architecture);
    }

    private void pkgListToBdioWithArch(final PackageManagerEnum pkgMgrType, final String linuxDistroName, final String pkgMgrListCmdOutputPath, final String bdioOutputPath, final String blackDuckProjectName,
        final String blackDuckProjectVersion, final String codeLocationName, final String architecture) throws IntegrationException {
        File pkgMgrListCmdOutputFile = new File(pkgMgrListCmdOutputPath);
        List<String> pkgMgrListCmdOutputLinesList;
        try {
            pkgMgrListCmdOutputLinesList = FileUtils.readLines(pkgMgrListCmdOutputFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error reading package manager list command output file %s", pkgMgrListCmdOutputFile.getAbsolutePath()), e);
        }
        String[] pkgMgrListCmdOutputLines = pkgMgrListCmdOutputLinesList.toArray(new String[pkgMgrListCmdOutputLinesList.size()]);
        String[] bdioLines = pkgListToBdioWithArch(pkgMgrType,  linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, architecture);
        File bdioOutputFile = new File(bdioOutputPath);
        try {
            FileUtils.writeLines(bdioOutputFile, Arrays.asList(bdioLines));
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error writing BDIO file %s", bdioOutputFile.getAbsolutePath()), e);
        }
    }

    private String[] pkgListToBdioWithArch(final PackageManagerEnum pkgMgrType, final String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String blackDuckProjectName, final String blackDuckProjectVersion,
        final String codeLocationName, final String architecture) throws IntegrationException {
        ComponentExtractor extractor = componentExtractorFactory.createComponentExtractor(gson, null, architecture, pkgMgrType);
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
