/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.none.NullPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm.RpmPkgMgr;

@Component
public class PkgMgrFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public PkgMgr createPkgMgr(final PackageManagerEnum packageManagerEnum, final String architecture) {
        logger.debug("createPkgMgr()");
        if (packageManagerEnum == PackageManagerEnum.APK) {
            return new ApkPkgMgr(new FileOperations(), architecture);
        } else if (packageManagerEnum == PackageManagerEnum.DPKG) {
            return new DpkgPkgMgr(new FileOperations());
        } else if (packageManagerEnum == PackageManagerEnum.RPM) {
            return new RpmPkgMgr(new Gson(), new FileOperations());
        } else {
            logger.info("No supported package manager found; will generate empty BDIO");
            return new NullPkgMgr();
        }
    }
}
