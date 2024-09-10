package com.blackduck.integration.blackduck.imageinspector.image.docker;

import com.blackduck.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerDetailsBuilder;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerMetadata;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DockerImageLayerMetadataExtractorTest {

    public static final String LAYER_0_EXTERNAL_ID = "layer0externalId";
    public static final String TEST_COMMAND = "test command";

    @Test
    void test() throws IntegrationException, IOException {
        DockerImageLayerConfigParser dockerImageLayerConfigParser = Mockito.mock(DockerImageLayerConfigParser.class);
        Mockito.when(dockerImageLayerConfigParser.parseCmd(Mockito.anyString())).thenReturn(Arrays.asList(TEST_COMMAND));

        DockerImageLayerMetadataExtractor dockerImageLayerMetadataExtractor = new DockerImageLayerMetadataExtractor(dockerImageLayerConfigParser);

        FullLayerMapping fullLayerMapping = Mockito.mock(FullLayerMapping.class);
        Mockito.when(fullLayerMapping.getLayerExternalId(0)).thenReturn(LAYER_0_EXTERNAL_ID);

        TypedArchiveFile layerTar = Mockito.mock(TypedArchiveFile.class);
        File layerTarFile = Mockito.mock(File.class);
        File layerTarParent = new File("src/test/resources/extraction/app/layerConfig/layerWithCmd");
        Mockito.when(layerTar.getFile()).thenReturn(layerTarFile);
        Mockito.when(layerTarFile.getParentFile()).thenReturn(layerTarParent);

        LayerDetailsBuilder layerData = new LayerDetailsBuilder(0, layerTar, LAYER_0_EXTERNAL_ID);
        LayerMetadata layerMetadata = dockerImageLayerMetadataExtractor.getLayerMetadata(fullLayerMapping, layerData);

        assertEquals(TEST_COMMAND, layerMetadata.getLayerCmd().get(0));
    }
}
