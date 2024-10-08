/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.api.name;

import com.blackduck.integration.blackduck.imageinspector.image.common.RepoTag;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ImageNameResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RepoTag resolve(@Nullable String foundImageName, @Nullable String givenRepo, @Nullable String givenTag) {
        if (StringUtils.isBlank(foundImageName)) {
            return new RepoTag(givenRepo, givenTag);
        }
        String resolvedImageRepo = givenRepo;
        String resolvedImageTag = givenTag;
        if (StringUtils.isNotBlank(foundImageName)) {
            resolvedImageTag = "latest";
            final int tagColonIndex = findColonBeforeTag(foundImageName);
            if (tagColonIndex < 0) {
                resolvedImageRepo = foundImageName;
            } else {
                resolvedImageRepo = foundImageName.substring(0, tagColonIndex);
                if (tagColonIndex + 1 != foundImageName.length()) {
                    resolvedImageTag = foundImageName.substring(tagColonIndex + 1);
                }
            }
        }
        return new RepoTag(resolvedImageRepo, resolvedImageTag);
    }

    private int findColonBeforeTag(final String givenImageName) {
        final int lastColonIndex = givenImageName.lastIndexOf(':');
        if (lastColonIndex < 0) {
            // This is just: repo (no tag)
            return -1;
        }
        final int lastSlashIndex = givenImageName.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            // This is a urlOrOrg/repo:tag
            return lastColonIndex;
        }
        if (lastColonIndex < lastSlashIndex) {
            // This colon is part of the repo (the colon before the port #)
            return -1;
        }
        return lastColonIndex;
    }
}
