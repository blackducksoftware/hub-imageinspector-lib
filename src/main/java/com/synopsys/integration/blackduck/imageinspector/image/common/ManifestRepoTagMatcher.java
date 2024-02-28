/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.common;

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
        logger.trace(String.format("Target repo tag %s; checking %s", targetRepoTag, manifestRepoTag));
        if (StringUtils.compare(manifestRepoTag, targetRepoTag) == 0) {
            logger.trace(String.format("Found the targetRepoTag %s", targetRepoTag));
            return true;
        }
        if (targetRepoTag.endsWith("/" + manifestRepoTag)) {
            logger.trace(String.format("Matched the targetRepoTag %s to %s by ignoring the repository prefix", targetRepoTag, manifestRepoTag));
            return true;
        }
        return false;
    }
}
