package com.blackduck.integration.blackduck.imageinspector.image.docker.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import com.blackduck.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import com.blackduck.integration.blackduck.imageinspector.image.common.ManifestRepoTagMatcher;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.synopsys.integration.exception.IntegrationException;

public class DockerManifestTest {

    @Test
    public void test() throws IOException, IntegrationException {
        final File tarExtractionDirectory = new File("src/test/resources/extraction");
        final String dockerTarFileName = "alpine.tar";
        DockerManifest manifest = new DockerManifest(new ManifestRepoTagMatcher(), new ImageNameResolver(), new File(tarExtractionDirectory, dockerTarFileName));
        final String targetImageName = "alpine";
        final String targetTagName = "latest";
        ManifestLayerMapping manifestLayerMapping = manifest.getLayerMapping(targetImageName, targetTagName);
        assertEquals("alpine", manifestLayerMapping.getImageName().get());
        assertEquals("latest", manifestLayerMapping.getTagName().get());
        assertEquals("caf27325b298a6730837023a8a342699c8b7b388b8d878966b064a1320043019.json", manifestLayerMapping.getPathToImageConfigFileFromRoot());
        assertEquals(1, manifestLayerMapping.getLayerInternalIds().size());
        assertEquals("03b951adf840798cb236a62db6705df7fb2f1e60e6f5fb93499ee8a566bd4114", manifestLayerMapping.getLayerInternalIds().get(0));
    }


    @Test
    public void testRepoIncludesRegistryPrefix() throws IOException, IntegrationException {
        final File tarExtractionDirectory = new File("src/test/resources/extraction");
        final String dockerTarFileName = "alpine.tar";
        DockerManifest manifest = new DockerManifest(new ManifestRepoTagMatcher(), new ImageNameResolver(), new File(tarExtractionDirectory, dockerTarFileName));
        final String targetImageName = "docker.io/alpine";
        final String targetTagName = "latest";
        ManifestLayerMapping manifestLayerMapping = manifest.getLayerMapping(targetImageName, targetTagName);
        assertEquals("alpine", manifestLayerMapping.getImageName().get());
        assertEquals("latest", manifestLayerMapping.getTagName().get());
        assertEquals("caf27325b298a6730837023a8a342699c8b7b388b8d878966b064a1320043019.json", manifestLayerMapping.getPathToImageConfigFileFromRoot());
        assertEquals(1, manifestLayerMapping.getLayerInternalIds().size());
        assertEquals("03b951adf840798cb236a62db6705df7fb2f1e60e6f5fb93499ee8a566bd4114", manifestLayerMapping.getLayerInternalIds().get(0));
    }
}
