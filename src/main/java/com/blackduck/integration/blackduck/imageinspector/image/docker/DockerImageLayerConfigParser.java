/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.docker;

import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.blackduck.integration.blackduck.imageinspector.image.common.ImageLayerConfigParser;
import org.springframework.stereotype.Component;

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
