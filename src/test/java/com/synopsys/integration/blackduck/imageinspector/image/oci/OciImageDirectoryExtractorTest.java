package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class OciImageDirectoryExtractorTest {
    private static String OCI_ALPINE_IMAGE_RESOURCE_PATH = "src/test/resources/oci/alpine";

    @Test
    public void testParseLayerArchives() {
        OciImageDirectoryExtractor extractor = new OciImageDirectoryExtractor(new GsonBuilder(), new FileOperations());
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
}
