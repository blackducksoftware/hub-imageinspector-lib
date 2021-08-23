/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.PackageGetter;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.image.docker.DockerImageDirectoryDataExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.image.docker.DockerImageFormatMatchesChecker;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentHierarchyBuilder;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class ImageInspectorApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private GsonBuilder gsonBuilder;
    private ImageInspector imageInspector;
    private Os os;
    private FileOperations fileOperations;
    private BdioGenerator bdioGenerator;
    private PkgMgrExecutor pkgMgrExecutor;
    private CmdExecutor cmdExecutor;

    public ImageInspectorApi(ImageInspector imageInspector, Os os) {
        this.imageInspector = imageInspector;
        this.os = os;
    }

    @Autowired
    public void setBdioGenerator(final BdioGenerator bdioGenerator) {
        this.bdioGenerator = bdioGenerator;
    }

    // autowired does not work on GsonBuilder; not sure why
    public void setGsonBuilder(final GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
    }

    @Autowired
    public void setFileOperations(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    @Autowired
    public void setPkgMgrExecutor(PkgMgrExecutor pkgMgrExecutor) {
        this.pkgMgrExecutor = pkgMgrExecutor;
    }

    @Autowired
    public void setCmdExecutor(CmdExecutor cmdExecutor) {
        this.cmdExecutor = cmdExecutor;
    }

    /**
     * Get a BDIO object representing the packages found in the image in the given tarfile. If the tarfile contains
     * more than one image, givenImageRepo and givenImageTag are used to select an image. If containerFileSystemOutputPath
     * is provided, this method will also write the container filesystem (reconstructed as part of the processing
     * required to read the image's packages) to that file as a .tar.gz file.
     * @param imageInspectionRequest Required. The request details.
     * @return The generated BDIO object representing the componets (packages) read from the images's package manager database.
     * @throws IntegrationException, InterruptedException
     */
    public SimpleBdioDocument getBdio(ImageInspectionRequest imageInspectionRequest) throws IntegrationException, InterruptedException {
        if (gsonBuilder == null) {
            gsonBuilder = new GsonBuilder();
        }
        PackageGetter packageGetter = new PackageGetter(pkgMgrExecutor, cmdExecutor);
        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        return getBdio(componentHierarchyBuilder, imageInspectionRequest);
    }

    SimpleBdioDocument getBdio(ComponentHierarchyBuilder componentHierarchyBuilder, final ImageInspectionRequest imageInspectionRequest)
        throws IntegrationException, InterruptedException {
        logger.info("getBdio()");
        os.logMemory();
        final String effectivePlatformTopLayerExternalId;
        if (imageInspectionRequest.isOrganizeComponentsByLayer() || imageInspectionRequest.isIncludeRemovedComponents()) {
            // base image component exclusion not supported when either of these is true
            effectivePlatformTopLayerExternalId = null;
        } else {
            effectivePlatformTopLayerExternalId = imageInspectionRequest.getPlatformTopLayerExternalId();
        }

        File tempDir;
        try {
            tempDir = fileOperations.createTempDirectory(imageInspectionRequest.isCleanupWorkingDir());
        } catch (final IOException e) {
            throw new IntegrationException(String.format("Error creating temp dir: %s", e.getMessage()), e);
        }

        List<ImageDirectoryDataExtractorFactory> imageDirectoryDataExtractorFactories = Arrays.asList(new DockerImageDirectoryDataExtractorFactory(new DockerImageFormatMatchesChecker()));
        ImageInfoDerived imageInfoDerived = null;
        try {
            imageInfoDerived = imageInspector.inspectImage(imageDirectoryDataExtractorFactories, componentHierarchyBuilder, imageInspectionRequest,
                tempDir,
                effectivePlatformTopLayerExternalId);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error inspecting image: %s", e.getMessage()), e);
        } finally {
            if (imageInspectionRequest.isCleanupWorkingDir()) {
                logger.info(String.format("Deleting working dir %s", tempDir.getAbsolutePath()));
                fileOperations.deleteDirPersistently(tempDir);
            }
        }
        return imageInfoDerived.getBdioDocument();
    }
}
