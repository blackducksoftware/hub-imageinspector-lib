/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.synopsys.integration.exception.IntegrationException;

public class OciLayoutParser {
    private final Gson gson;

    public OciLayoutParser(Gson gson) {
        this.gson = gson;
    }

    public String parseOciVersion(String ociLayoutFileContents) throws IntegrationException {
        try {
            JsonObject ociLayoutJsonObj = gson.fromJson(ociLayoutFileContents, JsonObject.class);
            JsonPrimitive ociLayoutVersionJsonPrimitive = ociLayoutJsonObj.getAsJsonPrimitive("imageLayoutVersion");
            return ociLayoutVersionJsonPrimitive.getAsString();
        } catch (Exception e) {
            throw new IntegrationException(String.format("Error parsing oci-layout file contents: %s", ociLayoutFileContents), e);
        }
    }
}
