/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrInitializer;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrs;

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
                if (!NO_VALUE.equals(rpmPackage.getEpoch())) {
                    packageVersion = String.format("%s:%s", rpmPackage.getEpoch(), rpmPackage.getVersion());
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
        if (packageLine.startsWith("{") && packageLine.endsWith("}") && packageLine.contains("epoch:") && packageLine.contains("name:") && packageLine.contains("version:") && packageLine.contains("arch:")) {
            return true;
        }
        return false;
    }
}
