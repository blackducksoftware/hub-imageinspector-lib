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

@Component
public class DockerManifestFactory {

    public DockerManifest createManifest(final File tarExtractionDirectory) {
        final DockerManifest manifest = new DockerManifest(tarExtractionDirectory);
        return manifest;
    }

}
