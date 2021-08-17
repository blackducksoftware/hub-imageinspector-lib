package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.util.List;

import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerConfigParser;

@Component
public class OciImageConfigCommandParser implements ImageLayerConfigParser {
    private final String CONFIG_DATA_JSON_KEY = "config";
    private final CommonImageConfigParser imageConfigParser;

    public OciImageConfigCommandParser(final CommonImageConfigParser imageConfigParser) {
        this.imageConfigParser = imageConfigParser;
    }

    @Override
    public List<String> parseCmd(final String layerConfigFileContents) {
        return imageConfigParser.parseCmd(layerConfigFileContents, CONFIG_DATA_JSON_KEY);
    }
}
