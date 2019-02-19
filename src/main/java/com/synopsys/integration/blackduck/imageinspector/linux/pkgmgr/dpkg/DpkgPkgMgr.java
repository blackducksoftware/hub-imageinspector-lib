package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.exception.IntegrationException;

public class DpkgPkgMgr implements PkgMgr {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String STANDARD_PKG_MGR_DIR_PATH = "/var/lib/dpkg";
    private final File inspectorPkgMgrDir;

    public DpkgPkgMgr() {
        this.inspectorPkgMgrDir = new File(STANDARD_PKG_MGR_DIR_PATH);
    }

    public DpkgPkgMgr(final String inspectorPkgMgrDirPath) {
        this.inspectorPkgMgrDir = new File(inspectorPkgMgrDirPath);
    }

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
            PackageManagerEnum.DPKG);
        return targetImagePkgMgr;
    }

    @Override
    public List<ComponentDetails> extractComponentsFromPkgMgrOutput(File imageFileSystem,
        String linuxDistroName, String[] pkgMgrListOutputLines)
        throws IntegrationException {
        return null;
    }

    private File getExtractedPackageManagerDirectory(File targetImageFileSystemRootDir) {
        return new File(targetImageFileSystemRootDir, STANDARD_PKG_MGR_DIR_PATH);
    }
}
