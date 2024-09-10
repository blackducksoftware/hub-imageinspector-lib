package com.blackduck.integration.blackduck.imageinspector.image.common.layerentry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.synopsys.integration.exception.IntegrationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerDataExtractor;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerDetailsBuilder;
import com.blackduck.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.blackduck.integration.blackduck.imageinspector.image.oci.OciImageLayerSorter;

public class LayerDataExtractorTest {

    public static final String SHA_256_PREFIX = "sha256:";

    @Test
    public void testOciLayerDataOrderedCorrectly() throws IntegrationException {
        String testRepo = "testRepo";
        String testTag = "testTag";
        String pathToConfig = "path";

        String internalFilenameA = "a";
        String internalFilenameB = "b";
        String internalFilenameC = "c";

        String internalIdA = SHA_256_PREFIX + internalFilenameA;
        String internalIdB = SHA_256_PREFIX + internalFilenameB;
        String internalIdC = SHA_256_PREFIX + internalFilenameC;
        List<String> layerInternalIds = Arrays.asList(
            internalIdA,
            internalIdB,
            internalIdC
        );
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(testRepo, testTag, pathToConfig, layerInternalIds);

        String externalIdA = "A";
        String externalIdB = "B";
        String externalIdC = "C";
        List<String> layerExternalIds = Arrays.asList(
            externalIdA,
            externalIdB,
            externalIdC
        );
        FullLayerMapping fullLayerMapping = new FullLayerMapping(manifestLayerMapping, layerExternalIds);

        Map<String, TypedArchiveFile> unorderedArchives = new HashMap<>();
        File sha256Dir = new File("sha256");
        unorderedArchives.put(externalIdB, new TypedArchiveFile(ArchiveFileType.TAR, new File(sha256Dir, internalFilenameB)));
        unorderedArchives.put(externalIdC, new TypedArchiveFile(ArchiveFileType.TAR, new File(sha256Dir, internalFilenameC)));
        unorderedArchives.put(externalIdA, new TypedArchiveFile(ArchiveFileType.TAR, new File(sha256Dir, internalFilenameA)));

        LayerDataExtractor layerDataExtractor = new LayerDataExtractor(new OciImageLayerSorter());
        List<LayerDetailsBuilder> layerData = layerDataExtractor.getLayerData(new ArrayList<>(unorderedArchives.values()), fullLayerMapping);

        Assertions.assertEquals(3, layerData.size());
        Assertions.assertEquals(externalIdA, layerData.get(0).getExternalId());
        Assertions.assertEquals(externalIdB, layerData.get(1).getExternalId());
        Assertions.assertEquals(externalIdC, layerData.get(2).getExternalId());
    }

}
