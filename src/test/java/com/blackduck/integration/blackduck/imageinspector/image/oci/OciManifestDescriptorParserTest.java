package com.blackduck.integration.blackduck.imageinspector.image.oci;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackduck.integration.blackduck.imageinspector.image.oci.util.OciImageHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.blackduck.imageinspector.image.common.ManifestRepoTagMatcher;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciDescriptor;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.blackduck.integration.exception.IntegrationException;
import org.mockito.Mockito;

public class OciManifestDescriptorParserTest {
    String manifestMediaType = "application/vnd.oci.image.manifest.v1+json";

    @Test
    public void testFindMatchingManifest() throws IntegrationException {
        Map<String, String> annotations1 = new HashMap<>();
        annotations1.put("org.opencontainers.image.ref.name", "repo:tag");
        annotations1.put("dummy", "dummy");
        Map<String, String> platform = new HashMap<>();
        platform.put("architecture", "amd64");
        OciDescriptor manifest1 = new OciDescriptor(manifestMediaType, "", "", annotations1, platform);

        Map<String, String> annotations2 = new HashMap<>();
        annotations2.put("org.opencontainers.image.ref.name", "tag:repo");
        OciDescriptor manifest2 = new OciDescriptor(manifestMediaType, "", "", annotations2, platform);

        OciDescriptor manifest3 = new OciDescriptor(manifestMediaType, "", "", null, null);

        List<OciDescriptor> manifests = Arrays.asList(manifest1, manifest2, manifest3);
        OciImageIndex ociImageIndex = new OciImageIndex(manifests);

        OciManifestDescriptorParser parser = new OciManifestDescriptorParser(new ManifestRepoTagMatcher(), Mockito.mock(OciImageHelper.class));
        Assertions.assertEquals(manifest1, parser.getManifestDescriptor(ociImageIndex, "repo", "tag", null));
    }

    // Test disabled because if there is only one manifest found, we select that manifest and do not perform matching
    // @Test
    public void testThrowsExceptionWhenNoMatchingManifests() {
        List<OciDescriptor> manifests = Arrays.asList(
            new OciDescriptor(manifestMediaType, "", "", new HashMap<>(), new HashMap<>())
        );
        OciImageIndex ociImageIndex = new OciImageIndex(manifests);
        OciManifestDescriptorParser parser = new OciManifestDescriptorParser(new ManifestRepoTagMatcher(), Mockito.mock(OciImageHelper.class));
        Assertions.assertThrows(IntegrationException.class, () -> parser.getManifestDescriptor(ociImageIndex, "repo", "tag", null));
    }
}
