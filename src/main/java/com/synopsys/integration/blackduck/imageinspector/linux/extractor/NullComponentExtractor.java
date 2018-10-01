package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;

public class NullComponentExtractor implements ComponentExtractor {

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName) {
        return new ArrayList<>();
    }
}
