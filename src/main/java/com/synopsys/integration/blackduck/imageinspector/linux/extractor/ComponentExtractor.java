package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.exception.IntegrationException;

public interface ComponentExtractor {
    static final String EXTERNAL_ID_STRING_FORMAT = "%s/%s/%s";

    List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName)
            throws IntegrationException;
}
