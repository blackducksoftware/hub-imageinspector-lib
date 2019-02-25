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
package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.CmdExecutor;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class PkgMgrExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Long CMD_TIMEOUT = 120000L;
    private final ReentrantLock lock = new ReentrantLock();

    public String[] runPackageManager(final CmdExecutor executor, final PkgMgr pkgMgr, final ImagePkgMgrDatabase imagePkgMgrDatabase) throws IntegrationException {
        logger.debug("Requesting lock for package manager execution");
        lock.lock();
        logger.debug("Acquired lock for package manager execution");
        try {
            final File packageManagerDirectory = pkgMgr.getInspectorPackageManagerDirectory();
            if (packageManagerDirectory.exists()) {
                pkgMgr.getPkgMgrInitializer().initPkgMgrDir(packageManagerDirectory);
            }
            logger.debug(String.format("Copying %s to %s", imagePkgMgrDatabase.getExtractedPackageManagerDirectory().getAbsolutePath(), packageManagerDirectory.getAbsolutePath()));
            FileUtils
                .copyDirectory(imagePkgMgrDatabase.getExtractedPackageManagerDirectory(), packageManagerDirectory);
            final String[] pkgMgrListOutputLines = listPackages(executor, pkgMgr);
            logger.trace(String.format("Package count: %d", pkgMgrListOutputLines.length));
            return pkgMgrListOutputLines;
        } catch (IOException | InterruptedException e) {
            throw new IntegrationException(String.format("Error installing or querying image's package manager database", e.getMessage()), e);
        } finally {
            logger.debug("Finished package manager execution");
            lock.unlock();
            logger.debug("Released lock after package manager execution");
        }
    }

    private String[] listPackages(final CmdExecutor executor, final PkgMgr pkgMgr) throws IntegrationException, IOException, InterruptedException {
        String[] results;
        logger.debug("Executing package manager");
        try {
            results = executor.executeCommand(pkgMgr.getListCommand(), CMD_TIMEOUT);
            logger.debug(String.format("Command %s executed successfully", pkgMgr.getListCommand()));
        } catch (final Exception e) {
            if (pkgMgr.getUpgradeCommand() != null) {
                logger.warn(String.format("Error executing \"%s\": %s; Trying to upgrade package database by executing: %s", pkgMgr.getListCommand(), e.getMessage(), pkgMgr.getUpgradeCommand()));
                executor.executeCommand(pkgMgr.getUpgradeCommand(), CMD_TIMEOUT);
                results = executor.executeCommand(pkgMgr.getListCommand(), CMD_TIMEOUT);
                logger.debug(String.format("Command %s executed successfully on 2nd attempt (after db upgrade)", pkgMgr.getListCommand()));
            } else {
                logger.error(String.format("Error executing \"%s\": %s; No upgrade command has been provided for this package manager", pkgMgr.getListCommand(), e.getMessage()));
                throw e;
            }
        }
        logger.debug(String.format("Package manager reported %s package lines", results.length));
        return results;
    }
}
