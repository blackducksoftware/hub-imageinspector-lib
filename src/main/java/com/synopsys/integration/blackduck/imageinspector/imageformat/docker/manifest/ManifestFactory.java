/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest;

import java.io.File;

import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMappingFactory;

@Component
public class ManifestFactory {

    public Manifest createManifest(final File tarExtractionDirectory) {
        final Manifest manifest = new Manifest(tarExtractionDirectory);
        final ManifestLayerMappingFactory factory = new ManifestLayerMappingFactory();
        manifest.setManifestLayerMappingFactory(factory);
        return manifest;
    }

}
