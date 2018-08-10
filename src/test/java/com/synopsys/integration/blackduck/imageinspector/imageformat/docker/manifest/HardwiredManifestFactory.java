package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest;

import java.io.File;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.Manifest;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;

public class HardwiredManifestFactory implements ManifestFactory {

    @Override
    public Manifest createManifest(final File tarExtractionDirectory, final String dockerTarFileName) {
        final Manifest manifest = new Manifest(tarExtractionDirectory, dockerTarFileName);
        manifest.setManifestLayerMappingFactory(new HardwiredManifestLayerMappingFactory());
        return manifest;
    }

}
