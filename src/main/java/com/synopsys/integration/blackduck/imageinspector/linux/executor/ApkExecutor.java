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
package com.synopsys.integration.blackduck.imageinspector.linux.executor;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ApkComponentExtractor;
import java.io.File;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class ApkExecutor extends PkgMgrExecutor {
    @Override
    @PostConstruct
    public void init() {
        initValues(ApkComponentExtractor.UPGRADE_DATABASE_COMMAND, ApkComponentExtractor.LIST_COMPONENTS_COMMAND);
    }

    @Override
    protected void initPkgMgrDir(final File packageManagerDirectory) {
        FileOperations.deleteFilesOnly(packageManagerDirectory);
    }
}
