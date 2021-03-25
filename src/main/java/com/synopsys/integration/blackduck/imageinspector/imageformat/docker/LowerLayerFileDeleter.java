/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class LowerLayerFileDeleter {
    private List<String> filesAddedByCurrentLayer = new LinkedList<>();
    private int defaultSearchDepth = 5; //TODO - what default makes sense?

    public void deleteFilesAddedByLowerLayers(File file) {
        deleteFilesAddedByLowerLayers(file, defaultSearchDepth);
    }

    public void deleteFilesAddedByLowerLayers(File file, int depthToLookForFilesAddedByCurrentLayer) {
        if (null == file || !file.exists() || depthToLookForFilesAddedByCurrentLayer < 0 || filesAddedByCurrentLayer.isEmpty() ) {
            FileUtils.deleteQuietly(file);
            return;
        }

        List<File> children = getChildrenSafely(file);
        for (File child : children) {
            deleteFilesAddedByLowerLayers(child, depthToLookForFilesAddedByCurrentLayer - 1);
        }

        if (!filesAddedByCurrentLayer.contains(file.getAbsolutePath())) {
            FileUtils.deleteQuietly(file);
        }
    }

    private List<File> getChildrenSafely(File file) {
        if (!file.isDirectory()) {
            return Collections.emptyList();
        }

        File[] children = file.listFiles();
        if (null == children) {
            return Collections.emptyList();
        }
        return Arrays.asList(children);
    }

    public void addFilesAddedByCurrentLayer(List<String> files) {
        filesAddedByCurrentLayer.addAll(files);
    }
}
