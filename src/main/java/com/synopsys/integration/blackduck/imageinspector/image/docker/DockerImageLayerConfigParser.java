/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.docker;

import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerConfigParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component
public class DockerImageLayerConfigParser implements ImageLayerConfigParser {
    private final String CONFIG_DATA_JSON_KEY = "container_config";
    private final CommonImageConfigParser imageConfigParser;

    public DockerImageLayerConfigParser(final CommonImageConfigParser imageConfigParser) {
        this.imageConfigParser = imageConfigParser;
    }

    @Override
    public List<String> parseCmd(String layerConfigFileContents) {
        return imageConfigParser.parseCmd(layerConfigFileContents, CONFIG_DATA_JSON_KEY);
    }
}
