/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.docker.manifest;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.blackduck.integration.util.Stringable;

public class DockerImageInfo extends Stringable {

    @SerializedName("Config")
    public String config;

    @SerializedName("RepoTags")
    public List<String> repoTags;

    @SerializedName("Layers")
    public List<String> layers;
}
