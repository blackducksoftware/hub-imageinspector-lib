package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;

public class RpmExtractorBehavior implements ExtractorBehavior {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String PATTERN_FOR_VALID_PACKAGE_LINE = ".+-.+-.+\\..*";
    private final PackageManagerEnum packageManagerEnum = PackageManagerEnum.RPM;
    private final PkgMgrExecutor pkgMgrExecutor;

    public RpmExtractorBehavior(final PkgMgrExecutor pkgMgrExecutor) {
        this.pkgMgrExecutor = pkgMgrExecutor;
    }

    @Override
    public PkgMgrExecutor getPkgMgrExecutor() {
        return pkgMgrExecutor;
    }

    @Override
    public PackageManagerEnum getPackageManagerEnum() {
        return packageManagerEnum;
    }

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final String givenArch, final String[] packageList, final String preferredAliasNamespace) {
        final List<ComponentDetails> components = new ArrayList<>();
        for (final String packageLine : packageList) {
            if (valid(packageLine)) {
                // Expected format: name-versionpart1-versionpart2.arch
                final int lastDotIndex = packageLine.lastIndexOf('.');
                final String archFromPkgMgrOutput = packageLine.substring(lastDotIndex + 1);
                final int lastDashIndex = packageLine.lastIndexOf('-');
                final String nameVersion = packageLine.substring(0, lastDashIndex);
                final int secondToLastDashIndex = nameVersion.lastIndexOf('-');
                final String versionRelease = packageLine.substring(secondToLastDashIndex + 1, lastDotIndex);
                final String artifact = packageLine.substring(0, secondToLastDashIndex);
                final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, artifact, versionRelease, archFromPkgMgrOutput);
                logger.debug(String.format("Adding externalId %s to components list", externalId));
                components.add(new ComponentDetails(artifact, versionRelease, externalId, archFromPkgMgrOutput, preferredAliasNamespace));
            }
        }
        return components;
    }

    private boolean valid(final String packageLine) {
        return packageLine.matches(PATTERN_FOR_VALID_PACKAGE_LINE);
    }
}
