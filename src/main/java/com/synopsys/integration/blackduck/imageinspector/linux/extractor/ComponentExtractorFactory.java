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
package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.none.NullPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm.RpmPkgMgr;

@Component
public class ComponentExtractorFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public PkgMgr createComponentExtractor(final PackageManagerEnum packageManagerEnum, final String architecture) {
        logger.debug("createComponentExtractor()");
        if (packageManagerEnum == PackageManagerEnum.APK) {
            return new ApkPkgMgr(architecture);
        } else if (packageManagerEnum == PackageManagerEnum.DPKG) {
            return new DpkgPkgMgr();
        } else if (packageManagerEnum == PackageManagerEnum.RPM) {
            return new RpmPkgMgr();
        } else {
            logger.info("No supported package manager found; will generate empty BDIO");
            return new NullPkgMgr();
        }
    }
}
