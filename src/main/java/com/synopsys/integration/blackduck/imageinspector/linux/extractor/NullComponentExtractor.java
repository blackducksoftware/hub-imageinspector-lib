package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.hub.bdio.model.Forge;

public class NullComponentExtractor implements ComponentExtractor {
    private final static List<Forge> defaultForges = Arrays.asList(new Forge("/", "/", "unknown"));

    @Override
    public List<Forge> getDefaultForges() {
        return defaultForges;
    }

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String preferredAliasNamespace) {
        return new ArrayList<>();
    }
}
