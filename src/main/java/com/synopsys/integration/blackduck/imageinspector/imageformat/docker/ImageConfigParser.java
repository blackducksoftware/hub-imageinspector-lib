package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImageConfigParser {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private GsonBuilder gsonBuilder;

  @Autowired
  public void setGsonBuilder(final GsonBuilder gsonBuilder) {
    this.gsonBuilder = gsonBuilder;
  }

  public List<String> getLayerIdsFromImageConfigFile(final String imageConfigFileContents) {
    try {
      logger.debug(String.format("imageConfigFileContents: %s", imageConfigFileContents));
      JsonObject imageConfigJsonObj = gsonBuilder.create().fromJson(imageConfigFileContents, JsonObject.class);
      JsonObject rootFsJsonObj = imageConfigJsonObj.getAsJsonObject("rootfs");
      JsonArray layerIdsJsonArray = rootFsJsonObj.getAsJsonArray("diff_ids");
      final int numLayers = layerIdsJsonArray.size();
      final List<String> layerIds = new ArrayList<>(numLayers);
      for (int i=0; i < numLayers; i++) {
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
