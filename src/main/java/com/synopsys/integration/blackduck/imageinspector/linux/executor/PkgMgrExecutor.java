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

import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.exception.IntegrationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PkgMgrExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Long CMD_TIMEOUT = 120000L;
    private final ReentrantLock lock = new ReentrantLock();
    private List<String> upgradeCommand;
    private List<String> listPackagesCommandParts;

    public abstract void init();

    void initValues(final List<String> upgradeCommand, final List<String> listPackagesCommandParts) {
        this.upgradeCommand = upgradeCommand;
        this.listPackagesCommandParts = listPackagesCommandParts;
    }

    public String[] runPackageManager(final ImagePkgMgrDatabase imagePkgMgr) throws IntegrationException {
        logger.info("Requesting lock for package manager execution");
        lock.lock();
        logger.info("Acquired lock for package manager execution");
        try {
            final File packageManagerDirectory = new File(imagePkgMgr.getPackageManager().getDirectory());
            if (packageManagerDirectory.exists()) {
                initPkgMgrDir(packageManagerDirectory);
            }
            logger.debug(String.format("Copying %s to %s", imagePkgMgr.getExtractedPackageManagerDirectory().getAbsolutePath(), packageManagerDirectory.getAbsolutePath()));
            FileUtils.copyDirectory(imagePkgMgr.getExtractedPackageManagerDirectory(), packageManagerDirectory);
            final String[] packages = listPackages();
            logger.trace(String.format("Package count: %d", packages.length));
            return packages;
        } catch (IOException | InterruptedException e) {
            throw new IntegrationException(String.format("Error installing or querying image's package manager database", e.getMessage()), e);
        } finally {
            logger.info("Finished package manager execution");
            lock.unlock();
            logger.info("Released lock after package manager execution");
        }
    }

    protected abstract void initPkgMgrDir(final File packageManagerDirectory) throws IOException;

    private String[] listPackages() throws IntegrationException, IOException, InterruptedException {
        String[] results;
        logger.debug("Executing package manager");
        try {
            results = Executor.executeCommand(listPackagesCommandParts, CMD_TIMEOUT);
            logger.info(String.format("Command %s executed successfully", listPackagesCommandParts));
        } catch (final Exception e) {
            if (upgradeCommand != null) {
                logger.warn(String.format("Error executing \"%s\": %s; Trying to upgrade package database by executing: %s", listPackagesCommandParts, e.getMessage(), upgradeCommand));
                Executor.executeCommand(upgradeCommand, CMD_TIMEOUT);
                results = Executor.executeCommand(listPackagesCommandParts, CMD_TIMEOUT);
                logger.info(String.format("Command %s executed successfully on 2nd attempt (after db upgrade)", listPackagesCommandParts));
            } else {
                logger.error(String.format("Error executing \"%s\": %s; No upgrade command has been provided for this package manager", listPackagesCommandParts, e.getMessage()));
                throw e;
            }
        }
        logger.debug(String.format("Package manager reported %s package lines", results.length));
        return results;
    }

}
