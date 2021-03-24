/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ManifestLayerMappingFactory {

    public ManifestLayerMapping createManifestLayerMapping(final String imageName, final String tagName, final String imageConfigFilename, final List<String> layers) {
        return new ManifestLayerMapping(imageName, tagName, imageConfigFilename, layers);
    }

}
