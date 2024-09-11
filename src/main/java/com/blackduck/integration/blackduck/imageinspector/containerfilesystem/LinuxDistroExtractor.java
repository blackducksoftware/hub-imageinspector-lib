/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem;

import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.blackduck.integration.blackduck.imageinspector.linux.Os;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

// TODO need a test for this

@Component
public class LinuxDistroExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileOperations fileOperations;
    private Os os;

    @Autowired
    public LinuxDistroExtractor(FileOperations fileOperations, Os os) {
        this.fileOperations = fileOperations;
        this.os = os;
    }

    public Optional<String> extract(final File targetImageFileSystemRootDir) {
        final LinuxFileSystem extractedFileSys = new LinuxFileSystem(targetImageFileSystemRootDir, fileOperations);
        final Optional<File> etcDir = extractedFileSys.getEtcDir();
        if (!etcDir.isPresent()) {
            return Optional.empty();
        }
        return extractLinuxDistroNameFromEtcDir(etcDir.get());
    }

    Optional<String> extractLinuxDistroNameFromEtcDir(final File etcDir) {
        logger.trace(String.format("/etc directory: %s", etcDir.getAbsolutePath()));
        if (fileOperations.listFilesInDir(etcDir).length == 0) {
            logger.warn(String.format("Could not determine the Operating System because the /etc dir (%s) is empty", etcDir.getAbsolutePath()));
        }
        return os.getLinuxDistroNameFromEtcDir(etcDir);
    }
}
