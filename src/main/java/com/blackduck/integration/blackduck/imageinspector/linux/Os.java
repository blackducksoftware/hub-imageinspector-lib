/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.linux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.blackduck.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackduck.integration.exception.IntegrationException;

@Component
public class Os {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ImageInspectorOsEnum deriveOs(final String linuxDistroName) throws IntegrationException {
        final ImageInspectorOsEnum osEnum = ImageInspectorOsEnum.determineOperatingSystem(linuxDistroName);
        if (osEnum == null) {
            throw new IntegrationException(String.format("Unrecognized linux distro: %s", linuxDistroName));
        }
        logger.debug(String.format("Running on OS: %s", osEnum.toString()));
        return osEnum;
    }

    public void logMemory() {
        final Long total = Runtime.getRuntime().totalMemory();
        final Long free = Runtime.getRuntime().freeMemory();
        logger.debug(String.format("Heap: total: %d; free: %d", total, free));
    }

    public Optional<String> getLinuxDistroNameFromEtcDir(File etcDirFile) {
        List<File> etcDirFiles = Arrays.asList(etcDirFile.listFiles());
        List<String> etcDirFilenames = etcDirFiles.stream().map(File::getName).collect(Collectors.toList());

        if (isOracleLinux(etcDirFile, etcDirFilenames)) {
            logger.trace("Target image Linux distro is an Oracle Linux");
            return Optional.of("oracle_linux");
        }

        Optional<String> distroName = Optional.empty();
        distroName = tryToDetermineDistro(etcDirFile, etcDirFilenames, distroName, "redhat-release");
        if (!distroName.isPresent()) {
            distroName = tryToDetermineDistro(etcDirFile, etcDirFilenames, distroName, "lsb-release");
        }
        if (!distroName.isPresent()) {
            distroName = tryToDetermineDistro(etcDirFile, etcDirFilenames, distroName, "os-release");
        }

        return distroName;
    }

    private boolean isOracleLinux(File etcDirFile, List<String> etcDirFilenames) {
        logger.trace("Trying /etc/oracle-release or /etc/os-release");
        if (etcDirFilenames.contains("oracle-release")) {
            logger.trace("Found oracle-release");
            return true;
        }
        
        File releaseFile = new File(etcDirFile, "os-release");
        Optional<String> distroName = getLinuxDistroNameFromStandardReleaseFile(releaseFile);

        return distroName.isPresent() && "ol".equalsIgnoreCase(distroName.get());
    }

    private Optional<String> tryToDetermineDistro(File etcDirFile, List<String> etcDirFilenames, Optional<String> distroName, String targetReleaseFilename) {
        logger.trace("Trying release file candidate: /etc/{}", targetReleaseFilename);
        if (etcDirFilenames.contains(targetReleaseFilename)) {
            File releaseFile = new File(etcDirFile, targetReleaseFilename);
            distroName = getLinxDistroName(releaseFile);
        }
        return distroName;
    }

    private Optional<String> getLinxDistroName(final File etcDirFile) {
        try {
            if ("redhat-release".equals(etcDirFile.getName())) {
                return getLinuxDistroNameFromRedHatReleaseFile(etcDirFile);
            } else {
                return getLinuxDistroNameFromStandardReleaseFile(etcDirFile);
            }
        } catch (final Exception e) {
            logger.warn(String.format("Error reading or parsing Linux distribution-identifying file: %s: %s", etcDirFile, e.getMessage()));
            return Optional.empty();
        }
    }

    private Optional<String> getLinuxDistroNameFromStandardReleaseFile(final File etcDirFile) {
        String linePrefix = null;
        if ("lsb-release".equals(etcDirFile.getName())) {
            logger.trace("Found lsb-release");
            linePrefix = "DISTRIB_ID=";
        } else if ("os-release".equals(etcDirFile.getName())) {
            logger.trace("Found os-release");
            linePrefix = "ID=";
        } else {
            logger.warn(String.format("File %s is not a Linux distribution-identifying file", etcDirFile.getAbsolutePath()));
            return Optional.empty();
        }
        try {
            List<String> lines = null;
            lines = FileUtils.readLines(etcDirFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith(linePrefix)) {
                    final String[] parts = line.split("=");
                    final String distroName = parts[1].replace("\"", "").toLowerCase();
                    logger.debug(String.format("Found target image Linux distro name '%s' in file %s", distroName, etcDirFile.getAbsolutePath()));
                    return Optional.of(distroName);
                }
            }
        } catch (IOException e) {
            logger.error(String.format("Error reading %s", etcDirFile.getAbsolutePath()));
            return Optional.empty();
        }
        logger.warn(String.format("Did not find value for %s in %s", linePrefix, etcDirFile.getAbsolutePath()));
        return Optional.empty();
    }

    private Optional<String> getLinuxDistroNameFromRedHatReleaseFile(final File etcDirFile) {
        logger.trace("Found redhat-release");
        try {
            List<String> lines = null;
            lines = FileUtils.readLines(etcDirFile, StandardCharsets.UTF_8);
            if (!lines.isEmpty()) {
                String line = lines.get(0);
                if (line.startsWith("Red Hat")) {
                    logger.trace("Contents of redhat-release indicate RHEL");
                    return Optional.of("rhel");
                }
                if (line.startsWith("CentOS")) {
                    logger.trace("Contents of redhat-release indicate CentOS");
                    return Optional.of("centos");
                }
                if (line.startsWith("Fedora")) {
                    logger.trace("Contents of redhat-release indicate Fedora");
                    return Optional.of("fedora");
                }
                logger.warn(String.format("Found redhat-release file %s but don't understand the contents: '%s'", etcDirFile.getAbsolutePath(), line));
                return Optional.empty();
            }
        } catch (IOException e) {
            logger.error(String.format("Error reading %s", etcDirFile.getAbsolutePath()));
            return Optional.empty();
        }
        logger.warn(String.format("Unable to discern linux distro from %s", etcDirFile.getAbsolutePath()));
        return Optional.empty();
    }
}
