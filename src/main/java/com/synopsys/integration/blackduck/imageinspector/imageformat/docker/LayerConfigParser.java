package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class LayerConfigParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<String> parseCmd(final GsonBuilder gsonBuilder, final String layerConfigFileContents) {
        try {
            logger.debug(String.format("layerConfigFileContents: %s", layerConfigFileContents));
            JsonObject imageConfigJsonObj = gsonBuilder.create().fromJson(layerConfigFileContents, JsonObject.class);
            JsonObject containerConfigJsonObj = imageConfigJsonObj.getAsJsonObject("container_config");
            JsonArray cmdPartsJsonArray = containerConfigJsonObj.getAsJsonArray("Cmd");
            final int numParts = cmdPartsJsonArray.size();
            final List<String> cmdParts = new ArrayList<>(numParts);
            for (int i = 0; i < numParts; i++) {
                logger.debug(String.format("layer cmd part: %s", cmdPartsJsonArray.get(i).getAsString()));
                cmdParts.add(cmdPartsJsonArray.get(i).getAsString());
            }
            return cmdParts;
        } catch (Exception e) {
            logger.warn(String.format("Error parsing layer cmd from layer config file contents: %s", e.getMessage()));
        }
        return null;
    }
}
