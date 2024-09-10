package com.blackduck.integration.blackduck.imageinspector.image.oci;

import com.google.gson.Gson;
import com.blackduck.integration.blackduck.imageinspector.image.common.ManifestRepoTagMatcher;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OciImageIndexFileParserTest {

    @Test
    void testManifestDigest() throws IntegrationException {
        File indexFile = new File("src/test/resources/oci/index_json/zero_annotations/index.json");
        Gson gson = new Gson();
        FileOperations fileOperations = new FileOperations();
        OciImageIndexFileParser ociImageIndexFileParser = new OciImageIndexFileParser(gson, fileOperations);

        OciImageIndex ociImageIndex = ociImageIndexFileParser.loadIndex(indexFile);
        // TODO this test is now cluttered / testing different classes
        // TODO need a test class for OciManifestDescriptorParser
        OciManifestDescriptorParser ociManifestDescriptorParser = new OciManifestDescriptorParser(new ManifestRepoTagMatcher());
        String manifestDigest = ociManifestDescriptorParser.getManifestDescriptor(ociImageIndex, "", "").getDigest();
        //assertEquals(1, ociImageIndex.getManifests().size());
        //String manifestDigest = ociImageIndexFileParser.parseManifestFileDigestFromImageIndex(ociImageIndex);
        assertEquals("sha256:8bd1d67ebe6aeae405d824c21560ec9aa2371ed48aa0c4a833e4672cadb0cf3e", manifestDigest);
    }

    @Test
    void testNoAnnotations() throws IntegrationException {
        File indexFile = new File("src/test/resources/oci/index_json/zero_annotations/index.json");
        Gson gson = new Gson();
        FileOperations fileOperations = new FileOperations();
        OciImageIndexFileParser ociImageIndexFileParser = new OciImageIndexFileParser(gson, fileOperations);

        OciImageIndex ociImageIndex = ociImageIndexFileParser.loadIndex(indexFile);
        assertEquals(1, ociImageIndex.getManifests().size());
        assertFalse(ociImageIndex.getManifests().get(0).getAnnotations().isPresent());
    }

    @Test
    void testOneAnnotation() throws IntegrationException {
        File indexFile = new File("src/test/resources/oci/index_json/single_annotation/index.json");
        Gson gson = new Gson();
        FileOperations fileOperations = new FileOperations();
        OciImageIndexFileParser ociImageIndexFileParser = new OciImageIndexFileParser(gson, fileOperations);

        OciImageIndex ociImageIndex = ociImageIndexFileParser.loadIndex(indexFile);
        assertEquals(1, ociImageIndex.getManifests().size());
        assertTrue(ociImageIndex.getManifests().get(0).getAnnotations().isPresent());
        assertEquals(1, ociImageIndex.getManifests().get(0).getAnnotations().get().size());
        Map<String, String> annotations = ociImageIndex.getManifests().get(0).getAnnotations().get();
        assertEquals("localhost/centosplus:1", annotations.get("org.opencontainers.image.ref.name"));
        assertFalse(ociImageIndex.getManifests().get(0).getAnnotation("bogusKey").isPresent());
        assertEquals("localhost/centosplus:1", ociImageIndex.getManifests().get(0).getAnnotation("org.opencontainers.image.ref.name").get());
        assertEquals("localhost/centosplus:1", ociImageIndex.getManifests().get(0).getRepoTagString().get());
    }

    @Test
    void testTwoAnnotations() throws IntegrationException {
        File indexFile = new File("src/test/resources/oci/index_json/multiple_annotations/index.json");
        Gson gson = new Gson();
        FileOperations fileOperations = new FileOperations();
        OciImageIndexFileParser ociImageIndexFileParser = new OciImageIndexFileParser(gson, fileOperations);

        OciImageIndex ociImageIndex = ociImageIndexFileParser.loadIndex(indexFile);
        assertEquals(1, ociImageIndex.getManifests().size());

        assertTrue(ociImageIndex.getManifests().get(0).getAnnotations().isPresent());
        assertEquals(2, ociImageIndex.getManifests().get(0).getAnnotations().get().size());
    }
}
