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

    @Autowired
    private ApkExecutor apkExecutor;

    @Autowired
    private DpkgExecutor dpkgExecutor;

    @Autowired
    private RpmExecutor rpmExecutor;

    @Autowired
    private Gson gson;

    public ComponentExtractor createComponentExtractor(final File imageFileSystem, final PackageManagerEnum packageManagerEnum) {
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
