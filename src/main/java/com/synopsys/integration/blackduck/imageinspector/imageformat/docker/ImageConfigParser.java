/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component
public class ImageConfigParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<String> getLayerIdsFromImageConfigFile(final GsonBuilder gsonBuilder, final String imageConfigFileContents) {
        try {
            logger.debug(String.format("imageConfigFileContents: %s", imageConfigFileContents));
            JsonObject imageConfigJsonObj = gsonBuilder.create().fromJson(imageConfigFileContents, JsonObject.class);
            JsonObject rootFsJsonObj = imageConfigJsonObj.getAsJsonObject("rootfs");
            JsonArray layerIdsJsonArray = rootFsJsonObj.getAsJsonArray("diff_ids");
            final int numLayers = layerIdsJsonArray.size();
            final List<String> layerIds = new ArrayList<>(numLayers);
            for (int i = 0; i < numLayers; i++) {
                logger.debug(String.format("layer ID: %s", layerIdsJsonArray.get(i).getAsString()));
                layerIds.add(layerIdsJsonArray.get(i).getAsString());
            }
            return layerIds;
        } catch (Exception e) {
            logger.warn(String.format("Error logging image config file contents: %s", e.getMessage()));
        }
        return null;
    }
}
