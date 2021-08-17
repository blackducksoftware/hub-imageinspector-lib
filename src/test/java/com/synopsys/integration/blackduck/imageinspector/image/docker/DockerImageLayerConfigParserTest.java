package com.synopsys.integration.blackduck.imageinspector.image.docker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;

public class DockerImageLayerConfigParserTest {

    @Test
    public void testImageConfigParser() throws IOException {
        final String layerConfigFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/app/layerConfig/layerWithCmd/json"), StandardCharsets.UTF_8);
        CommonImageConfigParser commonImageConfigParser = new CommonImageConfigParser(new GsonBuilder());
        DockerImageLayerConfigParser parser = new DockerImageLayerConfigParser(commonImageConfigParser);
        List<String> layerCommandParts = parser.parseCmd(layerConfigFileContents);
        assertEquals(3, layerCommandParts.size());
        assertEquals("/bin/sh", layerCommandParts.get(0));
        assertEquals("-c", layerCommandParts.get(1));
        assertEquals("apt-get -y install python3.6", layerCommandParts.get(2));
    }

    @Test
    public void testImageConfigParserNoCmd() throws IOException {
        final String layerConfigFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/app/layerConfig/layerWithoutCmd/json"), StandardCharsets.UTF_8);
        CommonImageConfigParser commonImageConfigParser = new CommonImageConfigParser(new GsonBuilder());
        DockerImageLayerConfigParser parser = new DockerImageLayerConfigParser(commonImageConfigParser);
        List<String> layerCommandParts = parser.parseCmd(layerConfigFileContents);
        assertTrue(layerCommandParts.isEmpty());
    }
}
