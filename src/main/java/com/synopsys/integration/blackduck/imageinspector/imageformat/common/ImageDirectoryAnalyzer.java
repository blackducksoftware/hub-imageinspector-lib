/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.common;

import com.synopsys.integration.blackduck.imageinspector.lib.FullLayerMapping;
import com.synopsys.integration.exception.IntegrationException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ImageDirectoryAnalyzer {
    List<TypedArchiveFile> getLayerArchives(File imageDir) throws IOException;
    FullLayerMapping getLayerMapping(File imageDir, final String repo, final String tag) throws IntegrationException;
}
