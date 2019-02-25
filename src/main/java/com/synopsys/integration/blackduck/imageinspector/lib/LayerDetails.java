/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.imageinspector.linux.extraction.ComponentDetails;

public class LayerDetails {
    private final int layerIndex;
    private final String layerExternalId;
    private final String layerMetadataFileContents;
    private final List<String> layerCmd;
    private final List<ComponentDetails> components;

    public LayerDetails(final int layerIndex, final String layerExternalId, final String layerMetadataFileContents, final List<String> layerCmd, final List<ComponentDetails> components) {
        this.layerIndex = layerIndex;
        this.layerExternalId = layerExternalId;
        this.layerMetadataFileContents = layerMetadataFileContents;
        this.layerCmd = layerCmd;
        this.components = components;
    }

    public String getLayerMetadataFileContents() {
        return layerMetadataFileContents;
    }

    public List<String> getLayerCmd() {
        return layerCmd;
    }

    public List<ComponentDetails> getComponents() {
        return components;
    }

    public String getLayerIndexedName() {
        if (StringUtils.isBlank(layerExternalId)) {
            return String.format("Layer%02d", layerIndex);
        } else {
            return String.format("Layer%02d_%s", layerIndex, layerExternalId.replaceAll(":", "_"));
        }
    }
}
