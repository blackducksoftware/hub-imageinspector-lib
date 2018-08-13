/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.synopsys.integration.blackduck.imageinspector.linux.executor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgr;
import com.synopsys.integration.exception.IntegrationException;

public class ExecutorMock extends PkgMgrExecutor {

    File resourceFile;

    public ExecutorMock(final File resourceFile) {
        this.resourceFile = resourceFile;
    }

    @Override
    public String[] runPackageManager(final ImagePkgMgr imagePkgMgr) throws IntegrationException, IOException, InterruptedException {
        final String[] packages = listPackages();
        return packages;
    }

    String[] listPackages() throws IOException {
        final List<String> lines = FileUtils.readLines(resourceFile, StandardCharsets.UTF_8);
        return lines.toArray(new String[lines.size()]);
    }

    @Override
    public void init() {
    }
}
