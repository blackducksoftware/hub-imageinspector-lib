/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import com.blackduck.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.blackduck.integration.exception.IntegrationException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public interface ImageDirectoryExtractor {
    List<TypedArchiveFile> getLayerArchives(File imageDir, @Nullable String givenRepo, @Nullable String givenTag) throws IntegrationException;
    FullLayerMapping getLayerMapping(File imageDir, final String repo, final String tag) throws IntegrationException;
}
