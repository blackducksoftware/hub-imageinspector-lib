package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.Executor;
import com.synopsys.integration.exception.IntegrationException;

public class PkgMgrExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Long CMD_TIMEOUT = 120000L;
    private final ReentrantLock lock = new ReentrantLock();
    private List<String> upgradeCommand;
    private List<String> listPackagesCommandParts;

    public String[] runPackageManager(final PkgMgr pkgMgr, final ImagePkgMgrDatabase imagePkgMgrDatabase) throws IntegrationException {
        logger.info("Requesting lock for package manager execution");
        lock.lock();
        logger.info("Acquired lock for package manager execution");
        try {
            final File packageManagerDirectory = pkgMgr.getInspectorPackageManagerDirectory();
            if (packageManagerDirectory.exists()) {
                pkgMgr.getPkgMgrInitializer().initPkgMgrDir(packageManagerDirectory);
            }
            logger.debug(String.format("Copying %s to %s", imagePkgMgrDatabase.getExtractedPackageManagerDirectory().getAbsolutePath(), packageManagerDirectory.getAbsolutePath()));
            FileUtils
                .copyDirectory(imagePkgMgrDatabase.getExtractedPackageManagerDirectory(), packageManagerDirectory);
            final String[] pkgMgrListOutputLines = listPackages();
            logger.trace(String.format("Package count: %d", pkgMgrListOutputLines.length));
            return pkgMgrListOutputLines;
        } catch (IOException | InterruptedException e) {
            throw new IntegrationException(String.format("Error installing or querying image's package manager database", e.getMessage()), e);
        } finally {
            logger.info("Finished package manager execution");
            lock.unlock();
            logger.info("Released lock after package manager execution");
        }
    }

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
