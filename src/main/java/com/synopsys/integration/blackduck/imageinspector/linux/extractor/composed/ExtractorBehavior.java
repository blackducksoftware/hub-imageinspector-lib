package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.Forge;

public interface ExtractorBehavior {
    static final String EXTERNAL_ID_STRING_FORMAT = "%s/%s/%s";

    PkgMgrExecutor getPkgMgrExecutor();

    PackageManagerEnum getPackageManagerEnum();

    List<Forge> getDefaultForges();

    List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String preferredAliasNamespace)
            throws IntegrationException;
}
