package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;

public interface ExtractorBehavior {
    static final String EXTERNAL_ID_STRING_FORMAT = "%s/%s/%s";

    PkgMgrExecutor getPkgMgrExecutor();

    PackageManagerEnum getPackageManagerEnum();

    List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final String architecture, final String[] packageList, final String preferredAliasNamespace);
}
