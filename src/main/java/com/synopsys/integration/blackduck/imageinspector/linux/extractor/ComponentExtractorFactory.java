package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ApkExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.DpkgExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.RpmExecutor;

@Component
public class ComponentExtractorFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ApkExecutor apkExecutor;

    private DpkgExecutor dpkgExecutor;

    private RpmExecutor rpmExecutor;

    @Autowired
    public void setApkExecutor(final ApkExecutor apkExecutor) {
        this.apkExecutor = apkExecutor;
    }

    @Autowired
    public void setDpkgExecutor(final DpkgExecutor dpkgExecutor) {
        this.dpkgExecutor = dpkgExecutor;
    }

    @Autowired
    public void setRpmExecutor(final RpmExecutor rpmExecutor) {
        this.rpmExecutor = rpmExecutor;
    }

    public ComponentExtractor createComponentExtractor(final Gson gson, final File imageFileSystem, final PackageManagerEnum packageManagerEnum) {
        logger.debug("createComponentExtractor()");
        if (packageManagerEnum == PackageManagerEnum.APK) {
            return new ApkComponentExtractor(apkExecutor, imageFileSystem);
        } else if (packageManagerEnum == PackageManagerEnum.DPKG) {
            return new DpkgComponentExtractor(dpkgExecutor);
        } else if (packageManagerEnum == PackageManagerEnum.RPM) {
            return new RpmComponentExtractor(rpmExecutor, gson);
        } else {
            logger.info("No supported package manager found; will generate empty BDIO");
            return new NullComponentExtractor();
        }
    }
}
