/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import java.io.File;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

// Basic information about an image
public class ImageInfoParsed {
    private final File fileSystemRootDir;
    private final ImagePkgMgrDatabase pkgMgr;
    private final String linuxDistroName;

    public ImageInfoParsed(final File fileSystemRootDir, final ImagePkgMgrDatabase pkgMgr, final String linuxDistroName) {
        this.fileSystemRootDir = fileSystemRootDir;
        this.pkgMgr = pkgMgr;
        this.linuxDistroName = linuxDistroName;
    }

    public File getFileSystemRootDir() {
        return fileSystemRootDir;
    }

    public ImagePkgMgrDatabase getPkgMgr() {
        return pkgMgr;
    }

    public String getLinuxDistroName() {
        return linuxDistroName;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
