package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;

public class ApkExtractorBehavior implements ExtractorBehavior {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PackageManagerEnum packageManagerEnum = PackageManagerEnum.APK;
    private final PkgMgrExecutor pkgMgrExecutor;

    public ApkExtractorBehavior(final PkgMgrExecutor pkgMgrExecutor) {
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
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final String architecture, final ImagePkgMgrDatabase imagePkgMgrDatabase,
            final String preferredAliasNamespace) throws IntegrationException {
        final List<ComponentDetails> components = new ArrayList<>();
        final String[] packageList = getPkgMgrExecutor().runPackageManager(imagePkgMgrDatabase);
        for (final String packageLine : packageList) {
            if (!packageLine.toLowerCase().startsWith("warning")) {
                logger.trace(String.format("packageLine: %s", packageLine));
                // Expected format: component-versionpart1-versionpart2
                // component may contain dashes (often contains one).
                final String[] parts = packageLine.split("-");
                if (parts.length < 3) {
                    logger.warn(String.format("apk output contains an invalid line: %s", packageLine));
                    continue;
                }
                final String version = String.format("%s-%s", parts[parts.length - 2], parts[parts.length - 1]);
                logger.trace(String.format("version: %s", version));
                String component = "";
                for (int i = 0; i < parts.length - 2; i++) {
                    final String part = parts[i];
                    if (StringUtils.isNotBlank(component)) {
                        component += String.format("-%s", part);
                    } else {
                        component = part;
                    }
                }
                logger.trace(String.format("component: %s", component));
                // if a package starts with a period, ignore it. It's a virtual meta package and the version information is missing
                if (!component.startsWith(".")) {
                    final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, component, version, architecture);
                    logger.debug(String.format("Constructed externalId: %s", externalId));
                    components.add(new ComponentDetails(component, version, externalId, architecture, preferredAliasNamespace));
                }
            }
        }
        return components;
    }
}
