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

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ApkExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.DpkgExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.RpmExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComponentExtractorFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private FileOperations fileOperations;
    private ApkExecutor apkExecutor;
    private DpkgExecutor dpkgExecutor;
    private RpmExecutor rpmExecutor;

    @Autowired
    public void setApkExecutor(final ApkExecutor apkExecutor) {
        this.apkExecutor = apkExecutor;
    }

    @Autowired
    public void setDpkgExecutor(final DpkgExecutor dpkgExecutor) {
        this.dpkgExecutor = dpkgExecutor;
    }

    @Autowired
    public void setRpmExecutor(final RpmExecutor rpmExecutor) {
        this.rpmExecutor = rpmExecutor;
    }

    @Autowired
    public void setFileOperations(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    public ComponentExtractor createComponentExtractor(final Gson gson, final PkgMgr pkgMgr, final File imageFileSystem, final String architecture, final PackageManagerEnum packageManagerEnum) {
        logger.debug("createComponentExtractor()");
        if (packageManagerEnum == PackageManagerEnum.APK) {
            return new ApkComponentExtractor(pkgMgr, fileOperations, apkExecutor, imageFileSystem, architecture);
        } else if (packageManagerEnum == PackageManagerEnum.DPKG) {
            return new DpkgComponentExtractor(pkgMgr, dpkgExecutor);
        } else if (packageManagerEnum == PackageManagerEnum.RPM) {
            return new RpmComponentExtractor(pkgMgr, rpmExecutor, gson);
        } else {
            logger.info("No supported package manager found; will generate empty BDIO");
            return new NullComponentExtractor();
        }
    }
}
