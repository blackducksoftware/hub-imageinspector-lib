/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.lib.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerMetadata;

public interface ImageLayerMetadataParser {
    LayerMetadata getLayerMetadata(FullLayerMapping fullLayerMapping, TypedArchiveFile layerTar, int layerIndex);
}
