package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ApkExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.DpkgExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.RpmExecutor;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;

@Component
public class PkgMgrExtractorFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApkExecutor apkExecutor;

    @Autowired
    private DpkgExecutor dpkgExecutor;

    @Autowired
    private RpmExecutor rpmExecutor;

    public ExtractorComposed createExtractor(final File imageFileSystem, final PackageManagerEnum packageManagerEnum) {
        if (packageManagerEnum == PackageManagerEnum.APK) {
            final File pkgMgrDatabaseDir = new File(imageFileSystem, packageManagerEnum.getDirectory());
            final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(pkgMgrDatabaseDir, packageManagerEnum);
            final ExtractorBehavior extractor = new ApkExtractorBehavior(apkExecutor);
            return new ExtractorComposed(new SimpleBdioFactory(), extractor, imagePkgMgrDatabase);
        } else if (packageManagerEnum == PackageManagerEnum.DPKG) {
            final File pkgMgrDatabaseDir = new File(imageFileSystem, packageManagerEnum.getDirectory());
            final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(pkgMgrDatabaseDir, packageManagerEnum);
            final ExtractorBehavior extractor = new ApkExtractorBehavior(dpkgExecutor);
            return new ExtractorComposed(new SimpleBdioFactory(), extractor, imagePkgMgrDatabase);
        } else if (packageManagerEnum == PackageManagerEnum.RPM) {
            final File pkgMgrDatabaseDir = new File(imageFileSystem, packageManagerEnum.getDirectory());
            final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(pkgMgrDatabaseDir, packageManagerEnum);
            final ExtractorBehavior extractor = new RpmExtractorBehavior(rpmExecutor);
            return new ExtractorComposed(new SimpleBdioFactory(), extractor, imagePkgMgrDatabase);
        } else {
            logger.info("No supported package manager found; will generate empty BDIO");
            final ExtractorBehavior extractor = new NullExtractorBehavior();
            return new ExtractorComposed(new SimpleBdioFactory(), extractor, null);
        }
    }
}
