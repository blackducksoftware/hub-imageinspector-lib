/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component
public class CommonImageConfigParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GsonBuilder gsonBuilder;

    public CommonImageConfigParser(final GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
    }

    public List<String> parseExternalLayerIds(final String imageConfigFileContents) {
        try {
            logger.trace(String.format("imageConfigFileContents: %s", imageConfigFileContents));
            JsonObject imageConfigJsonObj = gsonBuilder.create().fromJson(imageConfigFileContents, JsonObject.class);
            JsonObject rootFsJsonObj = imageConfigJsonObj.getAsJsonObject("rootfs");
            JsonArray layerIdsJsonArray = rootFsJsonObj.getAsJsonArray("diff_ids");
            final int numLayers = layerIdsJsonArray.size();
            final List<String> layerIds = new ArrayList<>(numLayers);
            for (int i = 0; i < numLayers; i++) {
                logger.trace(String.format("layer ID: %s", layerIdsJsonArray.get(i).getAsString()));
                layerIds.add(layerIdsJsonArray.get(i).getAsString());
            }
            return layerIds;
        } catch (Exception e) {
            logger.warn(String.format("Error parsing external layer IDs from image config file contents: %s", e.getMessage()));
        }
        return new ArrayList<>(0);
    }

    public List<String> parseCmd(String layerConfigFileContents, String configDataJsonKey) {
        try {
            if (StringUtils.isBlank(layerConfigFileContents)) {
                return new ArrayList<>(0);
            }
            logger.trace(String.format("layerConfigFileContents: %s", layerConfigFileContents));
            JsonObject imageConfigJsonObj = gsonBuilder.create().fromJson(layerConfigFileContents, JsonObject.class);
            JsonObject containerConfigJsonObj = imageConfigJsonObj.getAsJsonObject(configDataJsonKey);
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

    public List<String> getExternalLayerIdsFromImageConfigFile(File imageDir, String pathToImageConfigFile) {
        try {
            final File imageConfigFile = new File(imageDir, pathToImageConfigFile);
            final String imageConfigFileContents = FileUtils.readFileToString(imageConfigFile, StandardCharsets.UTF_8);
            logger.trace(String.format("imageConfigFileContents (%s): %s", imageConfigFile.getName(), imageConfigFileContents));
            return parseExternalLayerIds(imageConfigFileContents);
        } catch (Exception e) {
            logger.warn(String.format("Error logging image config file contents: %s", e.getMessage()));
        }
        return new ArrayList<>(0);
    }
}
