/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ApkExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.DpkgExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.RpmExecutor;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;

@Component
public class BdioGeneratorFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApkExecutor apkExecutor;

    @Autowired
    private DpkgExecutor dpkgExecutor;

    @Autowired
    private RpmExecutor rpmExecutor;

    @Autowired
    private Gson gson;

    public BdioGenerator createExtractor(final File imageFileSystem, final PackageManagerEnum packageManagerEnum) {
        if (packageManagerEnum == PackageManagerEnum.APK) {
            final File pkgMgrDatabaseDir = new File(imageFileSystem, packageManagerEnum.getDirectory());
            final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(pkgMgrDatabaseDir, packageManagerEnum);
            final ComponentExtractor componentExtractor = new ApkComponentExtractor(apkExecutor, imageFileSystem);
            return new BdioGenerator(new SimpleBdioFactory(), componentExtractor, imagePkgMgrDatabase);
        } else if (packageManagerEnum == PackageManagerEnum.DPKG) {
            final File pkgMgrDatabaseDir = new File(imageFileSystem, packageManagerEnum.getDirectory());
            final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(pkgMgrDatabaseDir, packageManagerEnum);
            final ComponentExtractor componentExtractor = new DpkgComponentExtractor(dpkgExecutor);
            return new BdioGenerator(new SimpleBdioFactory(), componentExtractor, imagePkgMgrDatabase);
        } else if (packageManagerEnum == PackageManagerEnum.RPM) {
            final File pkgMgrDatabaseDir = new File(imageFileSystem, packageManagerEnum.getDirectory());
            final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(pkgMgrDatabaseDir, packageManagerEnum);
            final ComponentExtractor componentExtractor = new RpmComponentExtractor(rpmExecutor, gson);
            return new BdioGenerator(new SimpleBdioFactory(), componentExtractor, imagePkgMgrDatabase);
        } else {
            logger.info("No supported package manager found; will generate empty BDIO");
            final ComponentExtractor componentExtractor = new NullComponentExtractor();
            return new BdioGenerator(new SimpleBdioFactory(), componentExtractor, null);
        }
    }
}
