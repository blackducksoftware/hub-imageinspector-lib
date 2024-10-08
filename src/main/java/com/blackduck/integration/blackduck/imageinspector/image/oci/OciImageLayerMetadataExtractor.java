/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.blackduck.integration.blackduck.imageinspector.image.common.ImageLayerMetadataExtractor;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerDetailsBuilder;
import com.blackduck.integration.blackduck.imageinspector.image.common.LayerMetadata;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OciImageLayerMetadataExtractor implements ImageLayerMetadataExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final OciImageConfigCommandParser configCommandParser;

    public OciImageLayerMetadataExtractor(final OciImageConfigCommandParser configCommandParser) {
        this.configCommandParser = configCommandParser;
    }

    @Override
    public LayerMetadata getLayerMetadata(FullLayerMapping fullLayerMapping, LayerDetailsBuilder layerData) {
        File layerTar = layerData.getArchive().getFile();
        File configFile = findConfigFile(layerTar, fullLayerMapping.getManifestLayerMapping().getPathToImageConfigFileFromRoot());
        List<String> cmd = new LinkedList<>();
        try {
            String configFileContents = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
            cmd = configCommandParser.parseCmd(configFileContents);
        } catch (IOException e) {
            logger.trace(String.format("Unable to read contents of %s: %s", configFile.getAbsolutePath(), e.getMessage()));
        }
        return new LayerMetadata(cmd);
    }

    private File findConfigFile(File layerTar, String imageConfigFilePathFromRoot) {
        File blobsDir = layerTar.getParentFile().getParentFile();
        File imageRoot = blobsDir.getParentFile();
        return new File(imageRoot, imageConfigFilePathFromRoot);
    }
}
