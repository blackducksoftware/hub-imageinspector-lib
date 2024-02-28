/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.dpkg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.ComponentRelationshipPopulater;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk.ApkDbInfoFileParser;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.CommonRelationshipPopulater;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrInitializer;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrs;

@Component
public class DpkgPkgMgr implements PkgMgr {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final List<String> UPGRADE_DATABASE_COMMAND = null;
    private static final List<String> LIST_COMPONENTS_COMMAND = Arrays.asList("dpkg", "-l");
    private static final String PATTERN_FOR_COMPONENT_DETAILS_SEPARATOR = "[  ]+";
    private static final String PATTERN_FOR_LINE_PRECEDING_COMPONENT_LIST = "\\+\\+\\+-=+-=+-=+-=+";
    private static final String STANDARD_PKG_MGR_DIR_PATH = "/var/lib/dpkg";
    private static final String DB_INFO_FILE_PATH = "/var/lib/dpkg/status";
    private final PkgMgrInitializer pkgMgrInitializer;
    private final File inspectorPkgMgrDir;
    @Autowired
    public DpkgPkgMgr(final FileOperations fileOperations) {
        pkgMgrInitializer = new DpkgPkgMgrInitializer(fileOperations);
        this.inspectorPkgMgrDir = new File(STANDARD_PKG_MGR_DIR_PATH);
    }

    @Override
    public boolean isApplicable(File targetImageFileSystemRootDir) {
        final File packageManagerDirectory = getImagePackageManagerDirectory(targetImageFileSystemRootDir);
        final boolean applies = packageManagerDirectory.exists();
        logger.debug(String.format("%s %s", this.getClass().getName(), applies ? "applies" : "does not apply"));
        return applies;
    }

    @Override
    public PackageManagerEnum getType() {
        return PackageManagerEnum.DPKG;
    }

    @Override
    public PkgMgrInitializer getPkgMgrInitializer() {
        return pkgMgrInitializer;
    }

    @Override
    public File getImagePackageManagerDirectory(final File targetImageFileSystemRootDir) {
        return new File(targetImageFileSystemRootDir, STANDARD_PKG_MGR_DIR_PATH);
    }

    @Override
    public File getInspectorPackageManagerDirectory() {
        return inspectorPkgMgrDir;
    }

    @Override
    public List<String> getUpgradeCommand() {
        return UPGRADE_DATABASE_COMMAND;
    }

    @Override
    public List<String> getListCommand() {
        return LIST_COMPONENTS_COMMAND;
    }

    @Override
    public List<ComponentDetails> extractComponentsFromPkgMgrOutput(File imageFileSystem,
        String linuxDistroName, String[] pkgMgrListOutputLines) {
        final List<ComponentDetails> components = new ArrayList<>();
        boolean startOfComponents = false;
        for (final String packageLine : pkgMgrListOutputLines) {
            if (packageLine == null) {
                continue;
            }
            if (packageLine.matches(PATTERN_FOR_LINE_PRECEDING_COMPONENT_LIST)) {
                startOfComponents = true;
            } else if (startOfComponents) {
                // Expect: statusChar name version arch
                // Or: statusChar name:arch version arch
                final char packageStatus = packageLine.charAt(1);
                if (isInstalledStatus(packageStatus)) {
                    final String[] componentInfoParts = extractComponentInfoParts(packageLine);
                    final String component = extractComponent(componentInfoParts);
                    final String version = extractVersion(componentInfoParts);
                    final String archFromPkgMgrOutput = extractArch(componentInfoParts);
                    final String externalId = String.format(PkgMgrs.EXTERNAL_ID_STRING_FORMAT, component, version, archFromPkgMgrOutput);
                    logger.trace(String.format("Constructed externalId: %s", externalId));
                    components.add(new ComponentDetails(component, version, externalId, archFromPkgMgrOutput, linuxDistroName));
                } else {
                    logger.trace(String.format("Package \"%s\" is listed but not installed (package status: %s)", packageLine, packageStatus));
                }
            }
        }
        return components;
    }

    @Override
    public ComponentRelationshipPopulater createRelationshipPopulator(@Nullable CmdExecutor cmdExecutor) {
        return new CommonRelationshipPopulater(getRelationshipInfo());
    }

    public DbRelationshipInfo getRelationshipInfo() {
        DpkgDbInfoFileParser dpkgDbInfoFileParser = new DpkgDbInfoFileParser();
        File dbInfoFile = new File(DB_INFO_FILE_PATH);
        List<String> dbInfoFileLines = new LinkedList<>();
        try {
            dbInfoFileLines.addAll(Files.readAllLines(dbInfoFile.toPath()));
        } catch (IOException e) {
            logger.error(String.format("Unable to read file: %s", dbInfoFile.getAbsolutePath()));
            // if reading file fails, return object with empty maps
        }
        return dpkgDbInfoFileParser.parseDbRelationshipInfoFromFile(dbInfoFileLines);
    }

    private String extractComponent(final String[] componentInfoParts) {
        String name = componentInfoParts[0];
        if (name.contains(":")) {
            name = name.substring(0, name.indexOf(':'));
        }
        return name;
    }

    private String extractVersion(final String[] componentInfoParts) {
        return componentInfoParts[1];
    }

    private String extractArch(final String[] componentInfoParts) {
        return componentInfoParts[2];
    }




    private String[] extractComponentInfoParts(final String packageLine) {
        final String componentInfo = packageLine.substring(3);
        return componentInfo.trim().split(PATTERN_FOR_COMPONENT_DETAILS_SEPARATOR);
    }

    private boolean isInstalledStatus(final Character packageStatus) {
        return packageStatus == 'i' || packageStatus == 'W' || packageStatus == 't';
    }
}
