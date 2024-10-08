/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci;

import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.exception.IntegrationException;

import java.io.File;
import java.io.IOException;

public class OciImageIndexFileParser {
    private final Gson gson;
    private final FileOperations fileOperations;

    public OciImageIndexFileParser(Gson gson, FileOperations fileOperations) {
        this.gson = gson;
        this.fileOperations = fileOperations;
    }

    // TODO separate reading and parsing?

    public OciImageIndex loadIndex(File indexFile) throws IntegrationException {
        String indexFileText;
        try {
            indexFileText = fileOperations.readFileToString(indexFile);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error reading %s: %s", indexFile, e.getMessage()));
        }

        OciImageIndex imageIndex;
        try {
            imageIndex = gson.fromJson(indexFileText, OciImageIndex.class);
        } catch (JsonSyntaxException e) {
            throw new IntegrationException(String.format("Error parsing %s: %s", indexFile.getAbsolutePath(), e.getMessage()));
        }
        return imageIndex;
    }

}
