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

import com.synopsys.integration.util.Stringable;

public class ManifestLayerMapping extends Stringable {
    private final String imageName;
    private final String tagName;
    private final String imageConfigFilename;
    private final List<String> layerInternalIds;
    private final List<String> layerExternalIds;

    public ManifestLayerMapping(final String imageName, final String tagName, final String imageConfigFilename, final List<String> layers) {
        this.imageName = imageName;
        this.tagName = tagName;
        this.imageConfigFilename = imageConfigFilename;
        this.layerInternalIds = layers;
        this.layerExternalIds = null;
    }

    public ManifestLayerMapping(final ManifestLayerMapping partialManafestLayerMapping, final List<String> layerExternalIds) {
        this.imageName = partialManafestLayerMapping.getImageName();
        this.tagName = partialManafestLayerMapping.getTagName();
        this.imageConfigFilename = partialManafestLayerMapping.getImageConfigFilename();
        this.layerInternalIds = partialManafestLayerMapping.getLayerInternalIds();
        this.layerExternalIds = layerExternalIds;
    }

    public String getImageName() {
        return imageName;
    }

    public String getTagName() {
        return tagName;
    }

    public String getImageConfigFilename() {
        return imageConfigFilename;
    }

    public List<String> getLayerInternalIds() {
        return layerInternalIds;
    }

    public String getLayerExternalId(final int layerIndex) {
        if ((layerExternalIds == null) || (layerExternalIds.size() < layerIndex + 1)) {
            return null;
        }
        return layerExternalIds.get(layerIndex);
    }
}
