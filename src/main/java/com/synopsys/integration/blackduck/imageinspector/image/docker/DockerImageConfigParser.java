package com.synopsys.integration.blackduck.imageinspector.image.docker;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageConfigParser;

public class DockerImageConfigParser implements ImageConfigParser {
    private CommonImageConfigParser imageConfigParser;

    public DockerImageConfigParser(final CommonImageConfigParser imageConfigParser) {
        this.imageConfigParser = imageConfigParser;
    }

    @Override
    public List<String> parseExternalLayerIds(final String imageConfigFileContents) {
        return imageConfigParser.parseExternalLayerIds(imageConfigFileContents);
    }
}
