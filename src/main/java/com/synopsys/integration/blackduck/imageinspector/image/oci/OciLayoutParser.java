/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.synopsys.integration.exception.IntegrationException;

public class OciLayoutParser {
    private final GsonBuilder gsonBuilder;

    public OciLayoutParser(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
    }

    public String parseOciVersion(String ociLayoutFileContents) throws IntegrationException {
        try {
            JsonObject ociLayoutJsonObj = gsonBuilder.create().fromJson(ociLayoutFileContents, JsonObject.class);
            JsonPrimitive ociLayoutVersionJsonPrimitive = ociLayoutJsonObj.getAsJsonPrimitive("imageLayoutVersion");
            return ociLayoutVersionJsonPrimitive.getAsString();
        } catch (Exception e) {
            throw new IntegrationException(String.format("Error parsing oci-layout file contents: %s", ociLayoutFileContents), e);
        }
    }
}