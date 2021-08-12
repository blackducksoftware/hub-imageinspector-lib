/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.docker.manifest;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.synopsys.integration.util.Stringable;

public class DockerImageInfo extends Stringable {

    @SerializedName("Config")
    public String config;

    @SerializedName("RepoTags")
    public List<String> repoTags;

    @SerializedName("Layers")
    public List<String> layers;
}
