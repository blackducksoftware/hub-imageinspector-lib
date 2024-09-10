/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector;

import com.blackduck.integration.blackduck.imageinspector.api.name.Names;

import java.io.File;

public class WorkingDirectories {
    private static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private final File workingDir;

    public WorkingDirectories(File workingDir) {
        this.workingDir = workingDir;
    }

    public File getExtractedImageDir(File targetImageTarfile) {
        return new File(getImageExtractionBaseDirectory(), targetImageTarfile.getName());
    }

    private File getTargetImageFileSystemParentDir() {
        return new File(getImageExtractionBaseDirectory(), TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
    }

    public File getTargetImageFileSystemRootDir(String repo, String tag) {
        return new File(getTargetImageFileSystemParentDir(), Names.getTargetImageFileSystemRootDirName(repo, tag));
    }

    public File getTargetImageFileSystemAppLayersRootDir(String repo, String tag) {
        return new File(getTargetImageFileSystemParentDir(), Names.getTargetImageFileSystemAppLayersRootDirName(repo, tag));
    }

    private File getImageExtractionBaseDirectory() {
        return new File(workingDir, TAR_EXTRACTION_DIRECTORY);
    }
}
