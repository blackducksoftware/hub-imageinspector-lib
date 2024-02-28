/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.api.name;

import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.image.common.RepoTag;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ImageNameResolver {

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
