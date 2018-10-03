package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.output.RpmPackage;
import com.synopsys.integration.exception.IntegrationException;

public class RpmComponentExtractor implements ComponentExtractor {
    private static final String NO_VALUE = "(none)";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PkgMgrExecutor pkgMgrExecutor;
    private final Gson gson;

    public RpmComponentExtractor(final PkgMgrExecutor pkgMgrExecutor, final Gson gson) {
        this.pkgMgrExecutor = pkgMgrExecutor;
        this.gson = gson;
    }

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName)
            throws IntegrationException {
        final List<ComponentDetails> components = new ArrayList<>();
        final String[] packageList = pkgMgrExecutor.runPackageManager(imagePkgMgrDatabase);
        for (final String packageLine : packageList) {
            if (valid(packageLine)) {
                final RpmPackage rpmPackage = gson.fromJson(packageLine, RpmPackage.class);
                String packageName = rpmPackage.getName();
                if (!NO_VALUE.equals(rpmPackage.getEpoch())) {
                    packageName = String.format("%s:%s", rpmPackage.getEpoch(), rpmPackage.getName());
                }
                String arch = "";
                if (!NO_VALUE.equals(rpmPackage.getArch())) {
                    arch = rpmPackage.getArch();
                }
                final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, packageName, rpmPackage.getVersion(), arch);
                logger.debug(String.format("Adding externalId %s to components list", externalId));
                components.add(new ComponentDetails(packageName, rpmPackage.getVersion(), externalId, arch, linuxDistroName));
            }
        }
        return components;
    }

    private boolean valid(final String packageLine) {
        if (packageLine.startsWith("{") && packageLine.endsWith("}") && packageLine.contains("epoch:") && packageLine.contains("name:") && packageLine.contains("version:") && packageLine.contains("arch:")) {
            return true;
        }
        return false;
    }
}
