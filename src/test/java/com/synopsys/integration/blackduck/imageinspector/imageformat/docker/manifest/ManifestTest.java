package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;

public class ManifestTest {

    @Test
    public void test() throws IOException, IntegrationException {
        final File tarExtractionDirectory = new File("src/test/resources/extraction");
        final String dockerTarFileName = "alpine.tar";
        Manifest manifest = new Manifest(tarExtractionDirectory, dockerTarFileName);
        final ManifestLayerMappingFactory manifestLayerMappingFactory = new ManifestLayerMappingFactory();
        manifest.setManifestLayerMappingFactory(manifestLayerMappingFactory);
        final String targetImageName = "alpine";
        final String targetTagName = "latest";
        ManifestLayerMapping manifestLayerMapping = manifest.getLayerMapping(targetImageName, targetTagName);
        assertEquals("alpine", manifestLayerMapping.getImageName());
        assertEquals("latest", manifestLayerMapping.getTagName());
        assertEquals("caf27325b298a6730837023a8a342699c8b7b388b8d878966b064a1320043019.json", manifestLayerMapping.getConfig());
        assertEquals(1, manifestLayerMapping.getLayers().size());
        assertEquals("03b951adf840798cb236a62db6705df7fb2f1e60e6f5fb93499ee8a566bd4114", manifestLayerMapping.getLayers().get(0));
        // externalId is populated later when layer processed:
        assertEquals(null, manifestLayerMapping.getLayerExternalId(0));
    }
}
