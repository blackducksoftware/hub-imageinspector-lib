/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class PkgMgrExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Long CMD_TIMEOUT = 120000L;
    private final ReentrantLock lock = new ReentrantLock();

    public String[] runPackageManager(final CmdExecutor executor, final PkgMgr pkgMgr, final ImagePkgMgrDatabase imagePkgMgrDatabase) throws IntegrationException {
        logger.trace("Requesting lock for package manager execution");
        lock.lock();
        logger.trace("Acquired lock for package manager execution");
        try {
            final File packageManagerDirectory = pkgMgr.getInspectorPackageManagerDirectory();
            if (packageManagerDirectory.exists()) {
                pkgMgr.getPkgMgrInitializer().initPkgMgrDir(packageManagerDirectory);
            }
            logger.debug(String.format("Copying %s to %s", imagePkgMgrDatabase.getExtractedPackageManagerDirectory().getAbsolutePath(), packageManagerDirectory.getAbsolutePath()));
            FileUtils
                .copyDirectory(imagePkgMgrDatabase.getExtractedPackageManagerDirectory(), packageManagerDirectory, false);
            final String[] pkgMgrListOutputLines = listPackages(executor, pkgMgr);
            logger.trace(String.format("Package count: %d", pkgMgrListOutputLines.length));
            return pkgMgrListOutputLines;
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error installing or querying image's package manager database: %s", e.getMessage()), e);
        } finally {
            logger.debug("Finished package manager execution");
            lock.unlock();
            logger.trace("Released lock after package manager execution");
        }
    }

    private String[] listPackages(final CmdExecutor executor, final PkgMgr pkgMgr) throws IntegrationException, IOException {
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
