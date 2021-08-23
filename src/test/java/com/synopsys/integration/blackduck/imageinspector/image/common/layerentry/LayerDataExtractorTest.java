package com.synopsys.integration.blackduck.imageinspector.image.common.layerentry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.LayerDataExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.LayerDetailsBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.image.oci.OciImageLayerSorter;

public class LayerDataExtractorTest {
    @Test
    public void testOciLayerDataOrderedCorrectly() {
        String testRepo = "testRepo";
        String testTag = "testTag";
        String pathToConfig = "path";

        String internalIdA = "a";
        String internalIdB = "b";
        String internalIdC = "c";
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
        unorderedArchives.put(externalIdB, new TypedArchiveFile(ArchiveFileType.TAR, new File(internalIdB)));
        unorderedArchives.put(externalIdC, new TypedArchiveFile(ArchiveFileType.TAR, new File(internalIdC)));
        unorderedArchives.put(externalIdA, new TypedArchiveFile(ArchiveFileType.TAR, new File(internalIdA)));

        LayerDataExtractor layerDataExtractor = new LayerDataExtractor(new OciImageLayerSorter());
        List<LayerDetailsBuilder> layerData = layerDataExtractor.getLayerData(new ArrayList<>(unorderedArchives.values()), fullLayerMapping);

        Assertions.assertEquals(3, layerData.size());
        Assertions.assertEquals(externalIdA, layerData.get(0).getExternalId());
        Assertions.assertEquals(externalIdB, layerData.get(1).getExternalId());
        Assertions.assertEquals(externalIdC, layerData.get(2).getExternalId());
    }

}
