package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.LayerMetadata;
import com.synopsys.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;

public class OciImageLayerMetadataEctractorTest {
    @Test
    public void testGetLayerMetadata() {
        CommonImageConfigParser commonImageConfigParser = new CommonImageConfigParser(new GsonBuilder());
        OciImageConfigCommandParser configCommandParser = new OciImageConfigCommandParser(commonImageConfigParser);
        OciImageLayerMetadataExtractor metadataExtractor = new OciImageLayerMetadataExtractor(configCommandParser);

        String repo = "testRepo";
        String tag = "testTag";
        String pathToConfigFileFromRoot = "blobs/sha256/cdce9ebeb6e8364afeac430fe7a886ca89a90a5139bc3b6f40b5dbd0cf66391c";
        List<String> layerInternalIds = Arrays.asList("d3470daaa19c14ddf4ec500a3bb4f073fa9827aa4f19145222d459016ee9193e");
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(repo, tag, pathToConfigFileFromRoot, layerInternalIds);
        String externalId = "b2d5eeeaba3a22b9b8aa97261957974a6bd65274ebd43e1d81d0a7b8b752b116";
        List<String> layerExternalIds = Arrays.asList(externalId);
        FullLayerMapping fullLayerMapping = new FullLayerMapping(manifestLayerMapping, layerExternalIds);

        File layerTar = new File("src/test/resources/oci/alpine/blobs/sha256/d3470daaa19c14ddf4ec500a3bb4f073fa9827aa4f19145222d459016ee9193e");
        TypedArchiveFile typedArchiveFile = new TypedArchiveFile(ArchiveFileType.TAR_GZIPPED, layerTar);
        LayerMetadata metadata = metadataExtractor.getLayerMetadata(fullLayerMapping, typedArchiveFile, 0);

        Assertions.assertEquals(externalId, metadata.getLayerExternalId());
        Assertions.assertEquals(1, metadata.getLayerCmd().size());
        Assertions.assertEquals("/bin/sh", metadata.getLayerCmd().get(0));
    }
}
