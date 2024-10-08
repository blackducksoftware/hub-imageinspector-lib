/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ManifestRepoTagMatcher {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Optional<String> findMatch(List<String> manifestRepoTags, String targetRepoTag) {
        logger.debug(String.format("findRepoTag(): specifiedRepoTag: %s", targetRepoTag));
        for (final String repoTag : manifestRepoTags) {
            if (doesMatch(repoTag, targetRepoTag)) {
                return Optional.of(repoTag);
            }
        }
        return Optional.empty();
    }

    public Optional<String> findMatch(String manifestRepoTag, String targetRepoTag) {
        if (doesMatch(manifestRepoTag, targetRepoTag)) {
            return Optional.of(manifestRepoTag);
        } else {
            return Optional.empty();
        }
    }

    private boolean doesMatch(String manifestRepoTag, String targetRepoTag) {
        // targetRepoTag is always passed in the format of repo:tag to be compatible with all image formats
        // In case the manifestRepoTag is not in the repo:tag format, we check if this value matches the targetRepo or targetTag portion of targetRepoTag

        logger.trace(String.format("targetRepoTag value resolved as %s; comparing this value with manifestRepoTag %s", targetRepoTag, manifestRepoTag));
        if (!manifestRepoTag.contains(":") && targetRepoTag.contains(":")) {
            logger.trace(String.format("The manifestRepoTag %s is not in the format repo:tag. Checking if this value matches the targetRepo or targetTag portion of targetRepoTag", manifestRepoTag));
            String targetTag = StringUtils.substringAfterLast(targetRepoTag, ":");
            String targetRepo = StringUtils.substringBeforeLast(targetRepoTag, ":");
            if (StringUtils.compare(manifestRepoTag, targetRepo) == 0 || StringUtils.compare(manifestRepoTag, targetTag) == 0) {
                logger.trace(String.format("Matched the targetRepo (%s) or targetTag (%s) portion of targetRepoTag (%s) to manifestRepoTag (%s)", targetRepo, targetTag, targetRepoTag, manifestRepoTag));
                return true;
            }
        }
        if (StringUtils.compare(manifestRepoTag, targetRepoTag) == 0) {
            logger.trace(String.format("Matched the targetRepoTag %s to manifestRepoTag %s", targetRepoTag, manifestRepoTag));
            return true;
        }
        if (targetRepoTag.endsWith("/" + manifestRepoTag)) {
            logger.trace(String.format("Matched the targetRepoTag %s to manifestRepoTag %s by ignoring the repository prefix", targetRepoTag, manifestRepoTag));
            return true;
        }
        return false;
    }
}
