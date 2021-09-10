/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciDescriptor;
import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class OciImageIndexFileParser {
    private static final String MANIFEST_FILE_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";
    private final Gson gson;
    private final FileOperations fileOperations;

    public OciImageIndexFileParser(Gson gson, FileOperations fileOperations) {
        this.gson = gson;
        this.fileOperations = fileOperations;
    }

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

    // TODO do these methods really belong in the same class??

    public String parseManifestFileDigestFromImageIndex(OciImageIndex imageIndex) throws IntegrationException {
        String manifestFileDigest = null;
        for (OciDescriptor manifestData : imageIndex.getManifests()) {
            if (manifestData.getMediaType().equals(MANIFEST_FILE_MEDIA_TYPE)) {
                if (manifestFileDigest == null) {
                    manifestFileDigest = manifestData.getDigest();
                    break;
                } else {
                    //TODO- what to do if we find multiple manifests?  OCI specs mention sometimes there's one for each supported architecture
                    // we'd throw some kind of error, but should look into any pre-defined defaults that may inform us which one to pick (eg. re: architecture)
                    throw new RuntimeException(String.format("Found multiple manifest files: %s and %s.  Please specify which image to target.  See help for information on how to do so.", manifestFileDigest, manifestData.getDigest()));
                }
            }
        }
        // Per specs, the size of OciImageIndex.manifests may be 0, but we require it
        if (manifestFileDigest == null) {
            throw new IntegrationException(String.format("Manifest with media type %s not found in OCI image index", MANIFEST_FILE_MEDIA_TYPE));
        }
        return manifestFileDigest;
    }

}
