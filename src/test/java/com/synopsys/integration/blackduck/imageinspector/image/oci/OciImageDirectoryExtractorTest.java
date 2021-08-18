package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class OciImageDirectoryExtractorTest {
    private static String OCI_ALPINE_IMAGE_RESOURCE_PATH = "src/test/resources/oci/alpine";

    @Test
    public void testParseLayerArchives() {
        OciImageConfigParser configParser = new OciImageConfigParser(new CommonImageConfigParser(new GsonBuilder()));
        OciImageDirectoryExtractor extractor = new OciImageDirectoryExtractor(new GsonBuilder(), new FileOperations(), configParser);
        File alpineOciImageDir = new File(OCI_ALPINE_IMAGE_RESOURCE_PATH);
        try {
            List<TypedArchiveFile> layerArchives = extractor.getLayerArchives(alpineOciImageDir);
            Assertions.assertEquals(1, layerArchives.size());
            TypedArchiveFile archive = layerArchives.get(0);
            Assertions.assertEquals(new File(OCI_ALPINE_IMAGE_RESOURCE_PATH, "blobs/sha256/d3470daaa19c14ddf4ec500a3bb4f073fa9827aa4f19145222d459016ee9193e"), archive.getFile());
            Assertions.assertEquals(ArchiveFileType.TAR_GZIPPED, archive.getType());
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testGetLayerMapping() {
        OciImageConfigParser configParser = new OciImageConfigParser(new CommonImageConfigParser(new GsonBuilder()));
        OciImageDirectoryExtractor extractor = new OciImageDirectoryExtractor(new GsonBuilder(), new FileOperations(), configParser);
        File alpineOciImageDir = new File(OCI_ALPINE_IMAGE_RESOURCE_PATH);
        String testRepo = "testRepo";
        String testTag = "testTag";
        try {
            FullLayerMapping mapping = extractor.getLayerMapping(alpineOciImageDir, testRepo, testTag);
            List<String> externalIds = mapping.getLayerExternalIds();
            Assertions.assertEquals(1, externalIds.size());
            Assertions.assertEquals("sha256:b2d5eeeaba3a22b9b8aa97261957974a6bd65274ebd43e1d81d0a7b8b752b116", externalIds.get(0));

            ManifestLayerMapping manifestLayerMapping = mapping.getManifestLayerMapping();
            Assertions.assertEquals(testRepo, manifestLayerMapping.getImageName());
            Assertions.assertEquals(testTag, manifestLayerMapping.getTagName());
            Assertions.assertEquals("blobs/sha256/cdce9ebeb6e8364afeac430fe7a886ca89a90a5139bc3b6f40b5dbd0cf66391c", manifestLayerMapping.getImageConfigFilename());

            List<String> internalIds = manifestLayerMapping.getLayerInternalIds();
            Assertions.assertEquals(1, internalIds.size());
            Assertions.assertEquals("sha256:d3470daaa19c14ddf4ec500a3bb4f073fa9827aa4f19145222d459016ee9193e", internalIds.get(0));
        } catch (Exception e) {
            Assertions.fail();
        }
    }
}
