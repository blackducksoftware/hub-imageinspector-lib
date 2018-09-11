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
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ImageInfoParsed {
    private final String fileSystemRootDirName;
    private final ImagePkgMgr pkgMgr;
    private final String linuxDistroName;

    public ImageInfoParsed(final String fileSystemRootDirName, final ImagePkgMgr pkgMgr, final String linuxDistroName) {
        this.fileSystemRootDirName = fileSystemRootDirName;
        this.pkgMgr = pkgMgr;
        this.linuxDistroName = linuxDistroName;
    }

    public String getFileSystemRootDirName() {
        return fileSystemRootDirName;
    }

    public ImagePkgMgr getPkgMgr() {
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
