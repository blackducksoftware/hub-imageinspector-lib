package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.Forge;

public class ApkComponentExtractor implements ComponentExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String ARCH_FILENAME = "arch";
    private static final String ETC_SUBDIR_CONTAINING_ARCH = "apk";
    private final static List<Forge> defaultForges = Arrays.asList(OperatingSystemEnum.ALPINE.getForge());
    private final PkgMgrExecutor pkgMgrExecutor;
    private final File imageFileSystem;
    private String architecture;

    public ApkComponentExtractor(final PkgMgrExecutor pkgMgrExecutor, final File imageFileSystem) {
        this.pkgMgrExecutor = pkgMgrExecutor;
        this.imageFileSystem = imageFileSystem;
    }

    @Override
    public List<Forge> getDefaultForges() {
        return defaultForges;
    }

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgrDatabase imagePkgMgrDatabase,
            final String preferredAliasNamespace) throws IntegrationException {
        final List<ComponentDetails> components = new ArrayList<>();
        final String[] packageList = pkgMgrExecutor.runPackageManager(imagePkgMgrDatabase);
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
                    final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, component, version, getArchitecture());
                    logger.debug(String.format("Constructed externalId: %s", externalId));
                    components.add(new ComponentDetails(component, version, externalId, getArchitecture(), preferredAliasNamespace));
                }
            }
        }
        return components;
    }

    private String getArchitecture() throws IntegrationException {
        if (architecture == null) {
            architecture = "";
            final Optional<File> etc = new LinuxFileSystem(imageFileSystem).getEtcDir();
            if (etc.isPresent()) {
                final File apkDir = new File(etc.get(), ETC_SUBDIR_CONTAINING_ARCH);
                if (apkDir.isDirectory()) {
                    final File architectureFile = new File(apkDir, ARCH_FILENAME);
                    if (architectureFile.isFile()) {
                        try {
                            architecture = FileUtils.readLines(architectureFile, StandardCharsets.UTF_8).get(0).trim();
                        } catch (final IOException e) {
                            throw new IntegrationException(String.format("Error deriving architecture; cannot read %s: %s", architectureFile.getAbsolutePath(), e.getMessage()));
                        }
                    }
                }
            }
        }
        return architecture;
    }
}
