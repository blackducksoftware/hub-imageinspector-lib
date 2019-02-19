/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.linux.extraction.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extraction.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.extraction.ComponentExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class BdioGeneratorApi {
    // These must either match the KB's forge name, or be mapped to it in Forge.java
    public static final String LINUX_DISTRO_NAME_UBUNTU = "ubuntu";
    public static final String LINUX_DISTRO_NAME_DEBIAN = "debian";
    public static final String LINUX_DISTRO_NAME_CENTOS = "centos";
    public static final String LINUX_DISTRO_NAME_FEDORA = "fedora";
    public static final String LINUX_DISTRO_NAME_REDHAT = "redhat";
    public static final String LINUX_DISTRO_NAME_OPENSUSE = "opensuse";
    public static final String LINUX_DISTRO_NAME_ALPINE = "alpine";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ComponentExtractorFactory componentExtractorFactory;
    private Gson gson;
    private final BdioGenerator bdioGenerator;

    /**
     * @param gson
     * @param componentExtractorFactory
     * @param bdioGenerator
     */
    public BdioGeneratorApi(final Gson gson, final ComponentExtractorFactory componentExtractorFactory, final BdioGenerator bdioGenerator) {
        this.gson = gson;
        this.componentExtractorFactory = componentExtractorFactory;
        this.bdioGenerator = bdioGenerator;
    }

    /**
     * Convert packages (in the form of Linux package manager list command output that has been saved to a file) to a BDIO file
     * that can be uploaded to Black Duck. This method accepts packages from DPKG and RPM package managers.
     * @param pkgMgrType              The package manager used to generate the provided packages: PackageManagerEnum.DPKG or PackageManagerEnum.RPM
     * @param linuxDistroName         The name of the Linux distribution. This class provides values, as pubic fields, for popular Linux distros. For others, read the ID from /etc/os-release or /etc/lsb-release
     * @param pkgMgrListCmdOutputPath Path to file containing the output of dpkg -l, or rpm -qa --qf "\\{ epoch: \"%{E}\", name: \"%{N}\", version: \"%{V}-%{R}\", arch: \"%{ARCH}\" \\}\\n"
     * @param bdioOutputPath          The path to the output file to which this method will write the generated BDIO (JSON)
     * @param blackDuckProjectName    The Black Duck project for which the BDIO should be generated
     * @param blackDuckProjectVersion The Black Duck project version for which the BDIO should be generated
     * @param codeLocationName        The Code Location ("Scan") name for which the BDIO should be generated
     * @throws IntegrationException
     */
    public void pkgListToBdio(final PackageManagerEnum pkgMgrType, String linuxDistroName, final String pkgMgrListCmdOutputPath, final String bdioOutputPath, final String blackDuckProjectName, final String blackDuckProjectVersion,
        final String codeLocationName) throws IntegrationException {
        pkgListToBdioWithArch(pkgMgrType, linuxDistroName, pkgMgrListCmdOutputPath, bdioOutputPath, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, null);
    }

    /**
     * Convert packages (in the form of Linux package manager list command output, provided as an array of Strings) to BDIO (JSON)
     * that can be uploaded to Black Duck. This method accepts packages from DPKG and RPM package managers.
     * @param pkgMgrType               The package manager used to generate the provided packages: PackageManagerEnum.DPKG, PackageManagerEnum.RPM, or PackageManagerEnum.APK
     * @param linuxDistroName          The name of the Linux distribution. This class provides values, as pubic fields, for popular Linux distros. For others, read the ID from /etc/os-release or /etc/lsb-release
     * @param pkgMgrListCmdOutputLines The output of dpkg -l or rpm -qa --qf "\\{ epoch: \"%{E}\", name: \"%{N}\", version: \"%{V}-%{R}\", arch: \"%{ARCH}\" \\}\\n"
     * @param blackDuckProjectName     The Black Duck project for which the BDIO should be generated
     * @param blackDuckProjectVersion  The Black Duck project version for which the BDIO should be generated
     * @param codeLocationName         The Code Location ("Scan") name for which the BDIO should be generated
     * @return The BDIO generated from the given packages (JSON)
     * @throws IntegrationException
     */
    public String[] pkgListToBdio(final PackageManagerEnum pkgMgrType, String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName)
        throws IntegrationException {
        logger.info(String.format("pkgListToBdio(): pkgMgrType: %s; linuxDistroName: %s; pkgMgrListCmdOutputLines: %s, blackDuckProjectName: %s; blackDuckProjectVersion: %s; codeLocationName: %s",
            pkgMgrType, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName));

        if (pkgMgrType != PackageManagerEnum.DPKG && pkgMgrType != PackageManagerEnum.RPM) {
            throw new UnsupportedOperationException(("The pkgListToBdio() method only supports DPKG and RPM"));
        }
        return pkgListToBdioWithArch(pkgMgrType, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, null);
    }

    /**
     * Convert packages (in the form of "apk info -v" command output that has been saved to a file) to a BDIO file (JSON)
     * that can be uploaded to Black Duck. This method accepts packages from the APK package manager.
     * @param architecture            The output of "apk --print-arch", or contents of the /etc/apk/arch file
     * @param linuxDistroName         The value of the ID field from /etc/os-release
     * @param pkgMgrListCmdOutputPath The path of the file that contains the output of the command "apk info -v"
     * @param bdioOutputPath          The path to the output file to which this method will write the generated BDIO (JSON)
     * @param blackDuckProjectName    The Black Duck project for which the BDIO should be generated
     * @param blackDuckProjectVersion The Black Duck project version for which the BDIO should be generated
     * @param codeLocationName        The Code Location ("Scan") name for which the BDIO should be generated
     * @throws IntegrationException
     */
    public void pkgListToBdioApk(final String architecture, String linuxDistroName, final String pkgMgrListCmdOutputPath, final String bdioOutputPath, final String blackDuckProjectName, final String blackDuckProjectVersion,
        final String codeLocationName) throws IntegrationException {
        pkgListToBdioWithArch(PackageManagerEnum.APK, linuxDistroName, pkgMgrListCmdOutputPath, bdioOutputPath, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, architecture);
    }

    /**
     * Convert packages (in the form of "apk info -v" command output, provided as an array of Strings) to BDIO (JSON)
     * that can be uploaded to Black Duck. This method accepts packages from the APK package manager.
     * @param architecture             The output of "apk --print-arch", or contents of the /etc/apk/arch file
     * @param linuxDistroName          The value of the ID field from /etc/os-release
     * @param pkgMgrListCmdOutputLines The output of apk info -v
     * @param blackDuckProjectName     The Black Duck project for which the BDIO should be generated
     * @param blackDuckProjectVersion  The Black Duck project version for which the BDIO should be generated
     * @param codeLocationName         The Code Location ("Scan") name for which the BDIO should be generated
     * @return The BDIO generated from the given packages (JSON)
     * @throws IntegrationException
     */
    public String[] pkgListToBdioApk(final String architecture, String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationName)
        throws IntegrationException {
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
        String[] bdioLines = pkgListToBdioWithArch(pkgMgrType, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName, architecture);
        File bdioOutputFile = new File(bdioOutputPath);
        try {
            FileUtils.writeLines(bdioOutputFile, Arrays.asList(bdioLines));
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error writing BDIO file %s", bdioOutputFile.getAbsolutePath()), e);
        }
    }

    private String[] pkgListToBdioWithArch(final PackageManagerEnum pkgMgrType, final String linuxDistroName, final String[] pkgMgrListCmdOutputLines, final String blackDuckProjectName, final String blackDuckProjectVersion,
        final String codeLocationName, final String architecture) throws IntegrationException {
        final PkgMgr pkgMgr = componentExtractorFactory.createPkgMgr(pkgMgrType, architecture);
        List<ComponentDetails> comps = pkgMgr.extractComponentsFromPkgMgrOutput(null, linuxDistroName, pkgMgrListCmdOutputLines);
        logger.info(String.format("Extracted %d components from given package manager output", comps.size()));
        SimpleBdioDocument bdioDoc = bdioGenerator.generateFlatBdioDocumentFromComponents(codeLocationName, blackDuckProjectName, blackDuckProjectVersion, linuxDistroName, comps);
        try {
            return bdioGenerator.getBdioAsStringArray(bdioDoc);
        } catch (IOException e) {
            throw new IntegrationException("Error converting BDIO document to string array", e);
        }
    }
}
