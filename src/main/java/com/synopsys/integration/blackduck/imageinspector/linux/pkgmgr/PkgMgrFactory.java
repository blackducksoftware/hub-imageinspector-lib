/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
