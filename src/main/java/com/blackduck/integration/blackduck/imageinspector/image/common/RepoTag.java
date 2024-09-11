/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.common;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RepoTag {
    @Nullable
    private final String repo;
    @Nullable
    private final String tag;

    // TODO there are many more places where this class should be used

    public RepoTag(@Nullable String repo, @Nullable String tag) {
        this.repo = repo;
        this.tag = tag;
    }

    public Optional<String> getRepo() {
        // translate both null and "" to empty to simplify life for callers
        if (StringUtils.isBlank(repo)) {
            return Optional.empty();
        }
        return Optional.of(repo);
    }

    public Optional<String> getTag() {
        // translate both null and "" to empty to simplify life for callers
        if (StringUtils.isBlank(tag)) {
            return Optional.empty();
        }
        return Optional.of(tag);
    }
}
