/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component
public class DockerLayerConfigParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<String> parseCmd(final GsonBuilder gsonBuilder, final String layerConfigFileContents) {
        try {
            if (StringUtils.isBlank(layerConfigFileContents)) {
                return new ArrayList<>(0);
            }
            logger.trace(String.format("layerConfigFileContents: %s", layerConfigFileContents));
            JsonObject imageConfigJsonObj = gsonBuilder.create().fromJson(layerConfigFileContents, JsonObject.class);
            JsonObject containerConfigJsonObj = imageConfigJsonObj.getAsJsonObject("container_config");
            JsonArray cmdPartsJsonArray = containerConfigJsonObj.getAsJsonArray("Cmd");
            final int numParts = cmdPartsJsonArray.size();
            final List<String> cmdParts = new ArrayList<>(numParts);
            for (int i = 0; i < numParts; i++) {
                logger.trace(String.format("layer cmd part: %s", cmdPartsJsonArray.get(i).getAsString()));
                cmdParts.add(cmdPartsJsonArray.get(i).getAsString());
            }
            return cmdParts;
        } catch (Exception e) {
            logger.trace(String.format("Error parsing layer cmd from layer config file contents: %s", e.getMessage()));
        }
        return new ArrayList<>(0);
    }
}
