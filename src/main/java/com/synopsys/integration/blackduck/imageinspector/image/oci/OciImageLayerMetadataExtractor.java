package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageLayerMetadataExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.LayerMetadata;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;

public class OciImageLayerMetadataExtractor implements ImageLayerMetadataExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final OciImageConfigCommandParser configCommandParser;

    public OciImageLayerMetadataExtractor(final OciImageConfigCommandParser configCommandParser) {
        this.configCommandParser = configCommandParser;
    }

    @Override
    public LayerMetadata getLayerMetadata(final FullLayerMapping fullLayerMapping, final TypedArchiveFile layerTar, final int layerIndex) {
        File blobsDir = layerTar.getFile().getParentFile().getParentFile();
        File imageRoot = blobsDir.getParentFile();
        File configFile = new File(imageRoot, fullLayerMapping.getManifestLayerMapping().getImageConfigFilename());
        List<String> cmd = new LinkedList<>();
        try {
            String configFileContents = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
            cmd = configCommandParser.parseCmd(configFileContents);
        } catch (IOException e) {
            logger.trace(String.format("Unable to read contents of %s: %s", configFile.getAbsolutePath(), e.getMessage()));
        }

        String layerExternalId = fullLayerMapping.getLayerExternalId(layerIndex);
        return new LayerMetadata(layerExternalId, cmd);
    }
}
