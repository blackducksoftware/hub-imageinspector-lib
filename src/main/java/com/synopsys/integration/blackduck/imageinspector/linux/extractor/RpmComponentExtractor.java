package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.Forge;

public class RpmComponentExtractor implements ComponentExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String PATTERN_FOR_VALID_PACKAGE_LINE = ".+-.+-.+\\..*";
    private final static List<Forge> defaultForges = Arrays.asList(OperatingSystemEnum.CENTOS.getForge(), OperatingSystemEnum.FEDORA.getForge(), OperatingSystemEnum.REDHAT.getForge());
    private final PkgMgrExecutor pkgMgrExecutor;

    public RpmComponentExtractor(final PkgMgrExecutor pkgMgrExecutor) {
        this.pkgMgrExecutor = pkgMgrExecutor;
    }

    @Override
    public List<Forge> getDefaultForges() {
        return defaultForges;
    }

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String preferredAliasNamespace)
            throws IntegrationException {
        final List<ComponentDetails> components = new ArrayList<>();
        final String[] packageList = pkgMgrExecutor.runPackageManager(imagePkgMgrDatabase);
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
