/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.api.name;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.Nullable;

public class ImageNameResolver {

    public NameValuePair resolve(@Nullable String givenImageName) {
        Optional<String> newImageRepo = Optional.empty();
        Optional<String> newImageTag = Optional.empty();
        if (StringUtils.isNotBlank(givenImageName)) {
            newImageTag = Optional.of("latest");
            final int tagColonIndex = findColonBeforeTag(givenImageName);
            if (tagColonIndex < 0) {
                newImageRepo = Optional.of(givenImageName);
            } else {
                newImageRepo = Optional.of(givenImageName.substring(0, tagColonIndex));
                if (tagColonIndex + 1 != givenImageName.length()) {
                    newImageTag = Optional.of(givenImageName.substring(tagColonIndex + 1));
                }
            }
        }
        return new BasicNameValuePair(newImageRepo.orElse(""), newImageTag.orElse(""));
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
