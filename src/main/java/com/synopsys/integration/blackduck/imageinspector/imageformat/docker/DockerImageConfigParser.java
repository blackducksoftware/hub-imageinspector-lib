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

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ImageConfigParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component
public class DockerImageConfigParser implements ImageConfigParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public List<String> parseExternalLayerIds(final GsonBuilder gsonBuilder, final String imageConfigFileContents) {
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
}
