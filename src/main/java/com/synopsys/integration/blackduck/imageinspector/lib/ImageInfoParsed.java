/**
 * hub-imageinspector-lib
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.io.File;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;

// Basic information about an image
public class ImageInfoParsed {
    private final File fileSystemRootDir;
    private final ImagePkgMgrDatabase imagePkgMgrDatabase;
    private final String linuxDistroName;
    private final PkgMgr pkgMgr;

    public ImageInfoParsed(final File fileSystemRootDir, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName, PkgMgr pkgMgr) {
        this.fileSystemRootDir = fileSystemRootDir;
        this.imagePkgMgrDatabase = imagePkgMgrDatabase;
        this.linuxDistroName = linuxDistroName;
        this.pkgMgr = pkgMgr;
    }

    public File getFileSystemRootDir() {
        return fileSystemRootDir;
    }

    public ImagePkgMgrDatabase getImagePkgMgrDatabase() {
        return imagePkgMgrDatabase;
    }

    public String getLinuxDistroName() {
        return linuxDistroName;
    }

    public PkgMgr getPkgMgr() {
        return pkgMgr;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
