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
package com.blackducksoftware.integration.hub.imageinspector.linux;

import java.io.File;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.lib.PackageManagerEnum;

@Component
public class Os {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // TODO currentLinuxDistro should be Optional
    public OperatingSystemEnum deriveCurrentOs(final String currentLinuxDistro) throws HubIntegrationException {
        OperatingSystemEnum osEnum = OperatingSystemEnum.determineOperatingSystem(currentLinuxDistro);
        if (osEnum != null) {
            logger.debug(String.format("Using given value for current OS: %s", osEnum.toString()));
            return osEnum;
        }
        final String systemPropertyOsValue = System.getProperty("os.name");
        logger.debug(String.format("Deriving current OS; System.getProperty(\"os.name\") says: %s", systemPropertyOsValue));
        if (!isLinuxUnix(systemPropertyOsValue)) {
            throw new HubIntegrationException(String.format("System property OS value is '%s'; this appears to be a non-Linux/Unix system", systemPropertyOsValue));
        }
        final File rootDir = new File("/");
        final FileSys rootFileSys = new FileSys(rootDir);
        final Set<PackageManagerEnum> packageManagers = rootFileSys.getPackageManagers();
        if (packageManagers.size() == 1) {
            final PackageManagerEnum packageManager = packageManagers.iterator().next();
            osEnum = packageManager.getOperatingSystem();
            logger.debug(String.format("Current Operating System %s", osEnum.name()));
            return osEnum;
        }
        throw new HubIntegrationException(String.format("Unable to determine current operating system; %d package managers found: %s", packageManagers.size(), packageManagers));
    }

    private static boolean isLinuxUnix(final String osName) {
        if (osName == null) {
            return false;
        }
        return (osName.contains("nux") || osName.contains("nix") || osName.contains("aix"));
    }
}
