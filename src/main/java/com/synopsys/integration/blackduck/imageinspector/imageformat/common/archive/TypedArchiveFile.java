/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common.archive;

import java.io.File;

public class TypedArchiveFile {
    private final ArchiveFileType type;
    private final File file;

    public TypedArchiveFile(ArchiveFileType type, File file) {
        this.type = type;
        this.file = file;
    }

    public ArchiveFileType getType() {
        return type;
    }

    public File getFile() {
        return file;
    }
}
