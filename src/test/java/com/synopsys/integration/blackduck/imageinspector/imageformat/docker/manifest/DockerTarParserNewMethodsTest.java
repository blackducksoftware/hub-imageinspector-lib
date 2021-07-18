package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.lib.ContainerFileSystem;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO find a home for each of these
public class DockerTarParserNewMethodsTest {

    @Test
    public void testSelectLayerTar() {
        TypedArchiveFile layer0 = new TypedArchiveFile(ArchiveFileType.TAR, new File(new File("internalIdLayer0"), "layer.tar"));
        TypedArchiveFile layer1 = new TypedArchiveFile(ArchiveFileType.TAR, new File(new File("internalIdLayer1"), "layer.tar"));
        List<TypedArchiveFile> layerTars = Arrays.asList(layer1, layer0);
        ManifestLayerMapping manifestLayerMapping = Mockito.mock(ManifestLayerMapping.class);
        // manifestLayerMapping.getLayerInternalIds()
        List<String> internalIds = Arrays.asList("internalIdLayer0", "internalIdLayer1");
        Mockito.when(manifestLayerMapping.getLayerInternalIds()).thenReturn(internalIds);

        DockerTarParser dockerTarParser = new DockerTarParser();

        TypedArchiveFile selectedTar = dockerTarParser.selectLayerTar(layerTars, manifestLayerMapping, 0);
        assertEquals("internalIdLayer0", selectedTar.getFile().getParentFile().getName());

        selectedTar = dockerTarParser.selectLayerTar(layerTars, manifestLayerMapping, 1);
        assertEquals("internalIdLayer1", selectedTar.getFile().getParentFile().getName());
    }
}
