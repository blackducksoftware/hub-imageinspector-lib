package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;

public class DockerImageLayerConfigParserTest {

    @Test
    public void testImageConfigParser() throws IOException {
        final String layerConfigFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/app/layerConfig/layerWithCmd/json"), StandardCharsets.UTF_8);
        DockerImageLayerConfigParser parser = new DockerImageLayerConfigParser(new GsonBuilder());
        List<String> layerCommandParts = parser.parseCmd(layerConfigFileContents);
        assertEquals(3, layerCommandParts.size());
        assertEquals("/bin/sh", layerCommandParts.get(0));
        assertEquals("-c", layerCommandParts.get(1));
        assertEquals("apt-get -y install python3.6", layerCommandParts.get(2));
    }

    @Test
    public void testImageConfigParserNoCmd() throws IOException {
        final String layerConfigFileContents = FileUtils.readFileToString(new File("src/test/resources/extraction/app/layerConfig/layerWithoutCmd/json"), StandardCharsets.UTF_8);
        DockerImageLayerConfigParser parser = new DockerImageLayerConfigParser(new GsonBuilder());
        List<String> layerCommandParts = parser.parseCmd(layerConfigFileContents);
        assertTrue(layerCommandParts.isEmpty());
    }
}
