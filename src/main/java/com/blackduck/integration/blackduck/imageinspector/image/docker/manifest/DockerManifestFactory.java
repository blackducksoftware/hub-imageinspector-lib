/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.docker.manifest;

import java.io.File;

import com.blackduck.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import com.blackduck.integration.blackduck.imageinspector.image.common.ManifestRepoTagMatcher;
import org.springframework.stereotype.Component;

@Component
public class DockerManifestFactory {

    public DockerManifest createManifest(final File tarExtractionDirectory) {
        final DockerManifest manifest = new DockerManifest(new ManifestRepoTagMatcher(), new ImageNameResolver(), tarExtractionDirectory);
        return manifest;
    }

}
