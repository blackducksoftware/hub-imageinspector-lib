package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;

public class NullExtractorBehavior implements ExtractorBehavior {

    @Override
    public PkgMgrExecutor getPkgMgrExecutor() {
        return null;
    }

    @Override
    public PackageManagerEnum getPackageManagerEnum() {
        return PackageManagerEnum.NULL;
    }

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final String architecture, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String preferredAliasNamespace) {
        return new ArrayList<>();
    }

}
