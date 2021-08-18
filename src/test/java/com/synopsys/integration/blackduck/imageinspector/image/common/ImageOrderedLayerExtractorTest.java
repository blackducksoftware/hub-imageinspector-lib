package com.synopsys.integration.blackduck.imageinspector.image.common;

import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageOrderedLayerExtractorTest {

    @Test
    void test() {
        ImageOrderedLayerExtractor imageOrderedLayerExtractor = new ImageOrderedLayerExtractor();
        ManifestLayerMapping manifestLayerMapping = Mockito.mock(ManifestLayerMapping.class);
        List<String> layerInternalIds = Arrays.asList("layer0InternalId", "layer1InternalId");
        Mockito.when(manifestLayerMapping.getLayerInternalIds()).thenReturn(layerInternalIds);

        TypedArchiveFile layer0Archive = Mockito.mock(TypedArchiveFile.class);
        File layer0ArchiveFile = Mockito.mock(File.class);
        Mockito.when(layer0Archive.getFile()).thenReturn(layer0ArchiveFile);
        File layer0Dir = Mockito.mock(File.class);
        Mockito.when(layer0ArchiveFile.getParentFile()).thenReturn(layer0Dir);
        Mockito.when(layer0Dir.getName()).thenReturn("layer0InternalId");

        TypedArchiveFile layer1Archive = Mockito.mock(TypedArchiveFile.class);
        File layer1ArchiveFile = Mockito.mock(File.class);
        Mockito.when(layer1Archive.getFile()).thenReturn(layer1ArchiveFile);
        File layer1Dir = Mockito.mock(File.class);
        Mockito.when(layer1ArchiveFile.getParentFile()).thenReturn(layer1Dir);
        Mockito.when(layer1Dir.getName()).thenReturn("layer1InternalId");

        List<TypedArchiveFile> unOrderedLayerArchives = Arrays.asList(layer1Archive, layer0Archive);

         List<TypedArchiveFile> orderedLayerArchives = imageOrderedLayerExtractor.getOrderedLayerArchives(unOrderedLayerArchives, manifestLayerMapping);

         assertEquals(layer0Archive, orderedLayerArchives.get(0));
         assertEquals(layer1Archive, orderedLayerArchives.get(1));
    }
}
