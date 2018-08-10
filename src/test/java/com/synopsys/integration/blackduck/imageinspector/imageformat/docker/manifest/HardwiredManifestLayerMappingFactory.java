package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMappingFactory;

public class HardwiredManifestLayerMappingFactory implements ManifestLayerMappingFactory {

    @Override
    public ManifestLayerMapping createManifestLayerMapping(final String imageName, final String tagName, final List<String> layers) {
        final ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layers);
        return mapping;
    }

}
