package com.synopsys.integration.blackduck.imageinspector.image.common;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FullLayerMappingTest {

    @Test
    void test() {
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping("testImage", "testRepo",
                "testImageConfigFilename",
                Arrays.asList("layer0InternalId", "layer1InternalId", "layer2InternalId"));
        List<String> layerExternalIds = Arrays.asList("layer0ExternalId", "layer1ExternalId", "layer2ExternalId");
        FullLayerMapping fullLayerMapping = new FullLayerMapping(manifestLayerMapping, layerExternalIds);

        assertEquals("layer0ExternalId", fullLayerMapping.getLayerExternalId(0));
        assertEquals("layer1ExternalId", fullLayerMapping.getLayerExternalId(1));
        assertEquals("layer2ExternalId", fullLayerMapping.getLayerExternalId(2));
    }
}
