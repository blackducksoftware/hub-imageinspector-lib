/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.bdio.BdioGenerator;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.bdio.model.dependency.ProjectDependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.ComponentRelationshipPopulater;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrFactory;
import com.blackduck.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.blackduck.integration.exception.IntegrationException;

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
    private PkgMgrFactory pkgMgrFactory;
    private Gson gson;
    private final BdioGenerator bdioGenerator;

    /**
     * @param gson
     * @param pkgMgrFactory
     * @param bdioGenerator
     */
    public BdioGeneratorApi(Gson gson, PkgMgrFactory pkgMgrFactory, BdioGenerator bdioGenerator) {
        this.gson = gson;
        this.pkgMgrFactory = pkgMgrFactory;
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
    public void pkgListToBdio(
        PackageManagerEnum pkgMgrType,
        String linuxDistroName,
        String pkgMgrListCmdOutputPath,
        String bdioOutputPath,
        String blackDuckProjectName,
        String blackDuckProjectVersion,
        String codeLocationName
    ) throws IntegrationException {
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
    public String[] pkgListToBdio(
        PackageManagerEnum pkgMgrType,
        String linuxDistroName,
        String[] pkgMgrListCmdOutputLines,
        String blackDuckProjectName,
        String blackDuckProjectVersion,
        String codeLocationName
    )
        throws IntegrationException {
        logger.info(String.format(
            "pkgListToBdio(): pkgMgrType: %s; linuxDistroName: %s; pkgMgrListCmdOutputLines: %s, blackDuckProjectName: %s; blackDuckProjectVersion: %s; codeLocationName: %s",
            pkgMgrType,
            linuxDistroName,
            pkgMgrListCmdOutputLines,
            blackDuckProjectName,
            blackDuckProjectVersion,
            codeLocationName
        ));

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
    public void pkgListToBdioApk(
        String architecture,
        String linuxDistroName,
        String pkgMgrListCmdOutputPath,
        String bdioOutputPath,
        String blackDuckProjectName,
        String blackDuckProjectVersion,
        String codeLocationName
    ) throws IntegrationException {
        pkgListToBdioWithArch(
            PackageManagerEnum.APK,
            linuxDistroName,
            pkgMgrListCmdOutputPath,
            bdioOutputPath,
            blackDuckProjectName,
            blackDuckProjectVersion,
            codeLocationName,
            architecture
        );
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
    public String[] pkgListToBdioApk(
        String architecture,
        String linuxDistroName,
        String[] pkgMgrListCmdOutputLines,
        String blackDuckProjectName,
        String blackDuckProjectVersion,
        String codeLocationName
    )
        throws IntegrationException {
        logger.info(String.format(
            "pkgListToBdioApk(): architecture: %s; linuxDistroName: %s; pkgMgrListCmdOutputLines: %s, blackDuckProjectName: %s; blackDuckProjectVersion: %s; codeLocationName: %s",
            architecture,
            linuxDistroName,
            pkgMgrListCmdOutputLines,
            blackDuckProjectName,
            blackDuckProjectVersion,
            codeLocationName
        ));

        return pkgListToBdioWithArch(
            PackageManagerEnum.APK,
            linuxDistroName,
            pkgMgrListCmdOutputLines,
            blackDuckProjectName,
            blackDuckProjectVersion,
            codeLocationName,
            architecture
        );
    }

    private void pkgListToBdioWithArch(
        PackageManagerEnum pkgMgrType, String linuxDistroName, String pkgMgrListCmdOutputPath, String bdioOutputPath, String blackDuckProjectName,
        String blackDuckProjectVersion, String codeLocationName, String architecture
    ) throws IntegrationException {
        File pkgMgrListCmdOutputFile = new File(pkgMgrListCmdOutputPath);
        List<String> pkgMgrListCmdOutputLinesList;
        try {
            pkgMgrListCmdOutputLinesList = FileUtils.readLines(pkgMgrListCmdOutputFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error reading package manager list command output file %s", pkgMgrListCmdOutputFile.getAbsolutePath()), e);
        }
        String[] pkgMgrListCmdOutputLines = pkgMgrListCmdOutputLinesList.toArray(new String[pkgMgrListCmdOutputLinesList.size()]);
        String[] bdioLines = pkgListToBdioWithArch(
            pkgMgrType,
            linuxDistroName,
            pkgMgrListCmdOutputLines,
            blackDuckProjectName,
            blackDuckProjectVersion,
            codeLocationName,
            architecture
        );
        File bdioOutputFile = new File(bdioOutputPath);
        try {
            FileUtils.writeLines(bdioOutputFile, Arrays.asList(bdioLines));
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error writing BDIO file %s", bdioOutputFile.getAbsolutePath()), e);
        }
    }

    private String[] pkgListToBdioWithArch(
        PackageManagerEnum pkgMgrType,
        String linuxDistroName,
        String[] pkgMgrListCmdOutputLines,
        String blackDuckProjectName,
        String blackDuckProjectVersion,
        String codeLocationName,
        String architecture
    ) throws IntegrationException {
        PkgMgr pkgMgr = pkgMgrFactory.createPkgMgr(pkgMgrType, architecture);
        List<ComponentDetails> comps = pkgMgr.extractComponentsFromPkgMgrOutput(null, linuxDistroName, pkgMgrListCmdOutputLines);
        CmdExecutor cmdExecutor = new CmdExecutor(); //TODO- should this be constructor parameter?
        ComponentRelationshipPopulater relationshipPopulater = pkgMgr.createRelationshipPopulator(cmdExecutor);
        relationshipPopulater.populateRelationshipInfo(comps);
        logger.info(String.format("Extracted %d components from given package manager output", comps.size()));
        ExternalId projectExternalId = bdioGenerator.createProjectExternalId(blackDuckProjectName, blackDuckProjectVersion, linuxDistroName);
        ProjectDependency projectDependency = bdioGenerator.createProjectDependency(blackDuckProjectName, blackDuckProjectVersion, projectExternalId);
        SimpleBdioDocument bdioDoc = bdioGenerator.generateFlatBdioDocumentFromComponents(projectDependency, codeLocationName, projectExternalId, comps);
        try {
            return bdioGenerator.getBdioAsStringArray(bdioDoc);
        } catch (IOException e) {
            throw new IntegrationException("Error converting BDIO document to string array", e);
        }
    }
}
