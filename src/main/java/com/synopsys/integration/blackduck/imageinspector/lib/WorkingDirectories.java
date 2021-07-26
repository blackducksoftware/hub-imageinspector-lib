/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.synopsys.integration.blackduck.imageinspector.api.name.Names;

import java.io.File;

public class WorkingDirectories {
    // TODO I'm not sure these should stay public
    public static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    public static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private final File workingDir;

    public WorkingDirectories(File workingDir) {
        this.workingDir = workingDir;
    }

    public File getExtractedImageDir(File targetImageTarfile) {
        return new File(getImageExtractionBaseDirectory(), targetImageTarfile.getName());
    }

    // final File targetImageFileSystemParentDir = new File(tarExtractionBaseDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
    private File getTargetImageFileSystemParentDir() {
        return new File(getImageExtractionBaseDirectory(), TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
    }

    // final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageDirectoryData.getActualRepo(), imageDirectoryData.getActualTag()));
    public File getTargetImageFileSystemRootDir(String repo, String tag) {
        return new File(getTargetImageFileSystemParentDir(), Names.getTargetImageFileSystemRootDirName(repo, tag));
    }

    // targetImageFileSystemAppLayersRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemAppLayersRootDirName(imageDirectoryData.getActualRepo(), imageDirectoryData.getActualTag()));
    public File getTargetImageFileSystemAppLayersRootDir(String repo, String tag) {
        return new File(getTargetImageFileSystemParentDir(), Names.getTargetImageFileSystemAppLayersRootDirName(repo, tag));
    }

    private File getImageExtractionBaseDirectory() {
        return new File(workingDir, TAR_EXTRACTION_DIRECTORY);
    }
}
