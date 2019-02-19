package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class ApkPkgMgr implements PkgMgr {
    private static final String STANDARD_PKG_MGR_DIR_PATH = "/lib/apk";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final List<String> UPGRADE_DATABASE_COMMAND = null;
    public static final List<String> LIST_COMPONENTS_COMMAND = Arrays.asList("apk", "info", "-v");
    private static final String ARCH_FILENAME = "arch";
    private static final String ETC_SUBDIR_CONTAINING_ARCH = "apk";
    //  private final PkgMgrExecutor pkgMgrExecutor;
    //  private final File imageFileSystem;
    private String architecture;
    private final File inspectorPkgMgrDir;

    public ApkPkgMgr() {
        this.inspectorPkgMgrDir = new File(STANDARD_PKG_MGR_DIR_PATH);
    }

    public ApkPkgMgr(final String inspectorPkgMgrDirPath) {
        this.inspectorPkgMgrDir = new File(inspectorPkgMgrDirPath);
    }

    @Autowired
    private FileOperations fileOperations;

    @Override
    public boolean isApplicable(File targetImageFileSystemRootDir) {
        final File packageManagerDirectory = getExtractedPackageManagerDirectory(targetImageFileSystemRootDir);
        final boolean applies = packageManagerDirectory.exists();
        logger.debug(String.format("%s %s", this.getClass().getName(), applies ? "applies" : "does not apply"));
        return applies;
    }

    @Override
    public File getInspectorPackageManagerDirectory() {
        return inspectorPkgMgrDir;
    }

    @Override
    public ImagePkgMgrDatabase getImagePkgMgrDatabase(File targetImageFileSystemRootDir) {
        final File extractedPackageManagerDirectory = getExtractedPackageManagerDirectory(targetImageFileSystemRootDir);
        final ImagePkgMgrDatabase targetImagePkgMgr = new ImagePkgMgrDatabase(extractedPackageManagerDirectory,
            PackageManagerEnum.APK);
        return targetImagePkgMgr;
    }

    @Override
    public List<ComponentDetails> extractComponentsFromPkgMgrOutput(final File imageFileSystem, final String linuxDistroName,
        final String[] pkgMgrListOutputLines) throws IntegrationException {
        final List<ComponentDetails> components = new ArrayList<>();

        for (final String packageLine : pkgMgrListOutputLines) {
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
                    final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, component, version, getArchitecture(imageFileSystem));
                    logger.debug(String.format("Constructed externalId: %s", externalId));
                    components.add(new ComponentDetails(component, version, externalId, getArchitecture(imageFileSystem), linuxDistroName));
                }
            }
        }
        return components;
    }

    private File getExtractedPackageManagerDirectory(File targetImageFileSystemRootDir) {
        return new File(targetImageFileSystemRootDir, STANDARD_PKG_MGR_DIR_PATH);
    }

    private String getArchitecture(final File imageFileSystem) throws IntegrationException {
        if (architecture == null) {
            architecture = "";
            final Optional<File> etc = new LinuxFileSystem(imageFileSystem, fileOperations).getEtcDir();
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
