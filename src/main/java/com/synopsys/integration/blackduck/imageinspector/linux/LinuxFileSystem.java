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
package com.synopsys.integration.blackduck.imageinspector.linux;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.util.Stringable;

public class LinuxFileSystem extends Stringable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final File root;
    private final FileOperations fileOperations;

    public LinuxFileSystem(final File root, final FileOperations fileOperations) {
        this.root = root;
        this.fileOperations = fileOperations;
    }

    public Optional<File> getEtcDir() {
        logger.debug(String.format("Looking in root dir %s for etc dir", root.getAbsolutePath()));
        final File etcDir = new File(root, "etc");
        if (fileOperations.isDirectory(etcDir)) {
            return Optional.of(etcDir);
        } else {
            return Optional.empty();
        }
    }

    public void writeToTarGz(final File outputTarFile) throws IOException {
        CompressedFile.writeDirToTarGz(fileOperations, root, outputTarFile);
    }

}
