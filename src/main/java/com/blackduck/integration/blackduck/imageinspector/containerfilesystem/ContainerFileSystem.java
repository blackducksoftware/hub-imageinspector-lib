/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem;

import java.io.File;
import java.util.Optional;

import com.blackduck.integration.util.Stringable;

public class ContainerFileSystem extends Stringable {

    private final File targetImageFileSystemFull;
    private final File targetImageFileSystemAppOnly;

    public ContainerFileSystem(final File targetImageFileSystemFull) {
        this.targetImageFileSystemFull = targetImageFileSystemFull;
        this.targetImageFileSystemAppOnly = null;
    }

    public ContainerFileSystem(final File targetImageFileSystemFull, final File targetImageFileSystemAppOnly) {
        this.targetImageFileSystemFull = targetImageFileSystemFull;
        this.targetImageFileSystemAppOnly = targetImageFileSystemAppOnly;
    }

    public File getTargetImageFileSystemFull() {
        return targetImageFileSystemFull;
    }

    public Optional<File> getTargetImageFileSystemAppOnly() {
        return Optional.ofNullable(targetImageFileSystemAppOnly);
    }
}
