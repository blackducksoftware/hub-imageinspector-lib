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
package com.synopsys.integration.blackduck.imageinspector.linux;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class Os {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public OperatingSystemEnum deriveCurrentOs(final String currentLinuxDistro) throws IntegrationException {
        OperatingSystemEnum osEnum = OperatingSystemEnum.determineOperatingSystem(currentLinuxDistro);
        if (osEnum != null) {
            logger.debug(String.format("Using given value for current OS: %s", osEnum.toString()));
            return osEnum;
        }
        // TODO: The rest of this method can go away when docker exec mode goes away
        final String systemPropertyOsValue = System.getProperty("os.name");
        logger.debug(String.format("Deriving current OS; System.getProperty(\"os.name\") says: %s", systemPropertyOsValue));
        if (!isLinuxUnix(systemPropertyOsValue)) {
            throw new IntegrationException(String.format("System property OS value is '%s'; this appears to be a non-Linux/Unix system", systemPropertyOsValue));
        }
        final File rootDir = new File("/");
        final LinuxFileSystem rootFileSys = new LinuxFileSystem(rootDir);
        final Set<PackageManagerEnum> packageManagers = rootFileSys.getPackageManagers();
        if (packageManagers.size() == 1) {
            final PackageManagerEnum packageManager = packageManagers.iterator().next();
            osEnum = packageManager.getInspectorOperatingSystem();
            logger.debug(String.format("Current Operating System %s", osEnum.name()));
            return osEnum;
        }
        throw new IntegrationException(String.format("Unable to determine current operating system; %d package managers found: %s", packageManagers.size(), packageManagers));
    }

    public void logMemory() {
        final Long total = Runtime.getRuntime().totalMemory();
        final Long free = Runtime.getRuntime().freeMemory();
        logger.debug(String.format("Heap: total: %d; free: %d", total, free));
    }

    public boolean isLinuxDistroFile(final File candidate) {
        if ("lsb-release".equals(candidate.getName())) {
            return true;
        } else if ("os-release".equals(candidate.getName())) {
            return true;
        }
        return false;
    }

    public Optional<String> getLinxDistroName(final File linuxDistroFile) {
        try {
            String linePrefix = null;
            if ("lsb-release".equals(linuxDistroFile.getName())) {
                linePrefix = "DISTRIB_ID=";
            } else if ("os-release".equals(linuxDistroFile.getName())) {
                linePrefix = "ID=";
            } else {
                logger.warn(String.format("File %s is not a Linux distribution-identifying file", linuxDistroFile.getAbsolutePath()));
                return Optional.empty();
            }
            List<String> lines = null;
            lines = FileUtils.readLines(linuxDistroFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith(linePrefix)) {
                    final String[] parts = line.split("=");
                    final String distroName = parts[1].replaceAll("\"", "");
                    logger.info(String.format("Found target image Linux distro name '%s' in file %s", distroName, linuxDistroFile.getAbsolutePath()));
                    return Optional.of(distroName);
                }
            }
            logger.warn(String.format("Did not find value for %s in %s", linePrefix, linuxDistroFile.getAbsolutePath()));
            return Optional.empty();
        } catch (final Exception e) {
            logger.warn(String.format("Error reading or parsing Linux distribution-identifying file: %s: %s", linuxDistroFile, e.getMessage()));
            return Optional.empty();
        }
    }

    private boolean isLinuxUnix(final String osName) {
        if (osName == null) {
            return false;
        }
        return osName.contains("nux") || osName.contains("nix") || osName.contains("aix");
    }
}
