/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.rpm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.api.PackageManagerEnum;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.ComponentRelationshipPopulater;
import com.blackduck.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrInitializer;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrs;

@Component
public class RpmPkgMgr implements PkgMgr {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String STANDARD_PKG_MGR_DIR_PATH = "/var/lib/rpm";
    private static final String PACKAGE_FORMAT_STRING = "\\{ epoch: \"%{E}\", name: \"%{N}\", version: \"%{V}-%{R}\", arch: \"%{ARCH}\" \\}\\n";
    private static final List<String> UPGRADE_DATABASE_COMMAND = Arrays.asList("rpm", "--rebuilddb");
    private static final List<String> LIST_COMPONENTS_COMMAND = Arrays.asList("rpm", "-qa", "--qf", PACKAGE_FORMAT_STRING);
    private static final String NO_VALUE = "(none)";
    private final Gson gson;
    private final File inspectorPkgMgrDir;
    private final PkgMgrInitializer pkgMgrInitializer;

    @Autowired
    public RpmPkgMgr(final Gson gson, final FileOperations fileOperations) {
        this.gson = gson;
        pkgMgrInitializer = new RpmPkgMgrInitializer(fileOperations);
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
        return PackageManagerEnum.RPM;
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
        for (final String packageLine : pkgMgrListOutputLines) {
            if (valid(packageLine)) {
                final RpmPackage rpmPackage = gson.fromJson(packageLine, RpmPackage.class);
                final String packageName = rpmPackage.getName();
                String packageVersion = rpmPackage.getVersion();
                if (!NO_VALUE.equals(rpmPackage.getEpoch()) && rpmPackage.getEpoch() != null && !rpmPackage.getEpoch().equals("0")) {
                    packageVersion = String.format("%s:%s", rpmPackage.getEpoch(), packageVersion);
                }
                String arch = "";
                if (!NO_VALUE.equals(rpmPackage.getArch())) {
                    arch = rpmPackage.getArch();
                }
                final String externalId = String.format(PkgMgrs.EXTERNAL_ID_STRING_FORMAT, packageName, packageVersion, arch);
                logger.trace(String.format("Adding externalId %s to components list", externalId));
                components.add(new ComponentDetails(packageName, packageVersion, externalId, arch, linuxDistroName));
            }
        }
        return components;
    }

    private boolean valid(final String packageLine) {
        return packageLine.startsWith("{") && packageLine.endsWith("}") && packageLine.contains("epoch:") && packageLine.contains("name:") && packageLine.contains("version:") && packageLine.contains("arch:");
    }

    @Override
    public ComponentRelationshipPopulater createRelationshipPopulator(@Nullable CmdExecutor cmdExecutor) {
        return new RpmRelationshipPopulater(cmdExecutor);
    }

}
