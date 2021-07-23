/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ImageLayerArchiveAnalyzer;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.lib.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerMetadata;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DockerImageLayerArchiveAnalyzer implements ImageLayerArchiveAnalyzer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DOCKER_LAYER_METADATA_FILENAME = "json";
    private final DockerLayerConfigParser dockerLayerConfigParser;

    public DockerImageLayerArchiveAnalyzer(DockerLayerConfigParser dockerLayerConfigParser) {
        this.dockerLayerConfigParser = dockerLayerConfigParser;
    }

    @Override
    public LayerMetadata getLayerMetadata(FullLayerMapping fullLayerMapping, TypedArchiveFile layerTar, int layerIndex) {
        final String layerMetadataFileContents = getLayerMetadataFileContents(layerTar);
        final List<String> layerCmd = dockerLayerConfigParser.parseCmd(layerMetadataFileContents);
        String layerExternalId = fullLayerMapping.getLayerExternalId(layerIndex);
        return new LayerMetadata(layerExternalId, layerCmd);
    }

    private String getLayerMetadataFileContents(final TypedArchiveFile layerTarFile) {
        String layerMetadataFileContents = null;
        File dir = layerTarFile.getFile().getParentFile();
        File metadataFile = new File(dir, DOCKER_LAYER_METADATA_FILENAME);
        try {
            if (metadataFile.exists()) {
                layerMetadataFileContents = FileUtils.readFileToString(metadataFile, StandardCharsets.UTF_8);
                logger.trace(String.format("%s: %s", metadataFile.getAbsolutePath(), layerMetadataFileContents));
            }
        } catch (IOException e) {
            logger.trace(String.format("Unable to log contents of %s: %s", metadataFile.getAbsolutePath(), e.getMessage()));
        }
        return layerMetadataFileContents;
    }
}
