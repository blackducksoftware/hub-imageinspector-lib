/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.ImageInspector;
import com.blackduck.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.PackageGetter;
import com.blackduck.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.blackduck.integration.blackduck.imageinspector.image.common.ImageDirectoryDataExtractorFactory;
import com.blackduck.integration.blackduck.imageinspector.image.common.ImageInfoDerived;
import com.blackduck.integration.blackduck.imageinspector.image.docker.DockerImageDirectoryDataExtractorFactory;
import com.blackduck.integration.blackduck.imageinspector.image.docker.DockerImageFormatMatchesChecker;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentHierarchyBuilder;
import com.blackduck.integration.blackduck.imageinspector.image.oci.OciImageDirectoryDataExtractorFactory;
import com.blackduck.integration.blackduck.imageinspector.image.oci.OciImageFormatMatchesChecker;
import com.blackduck.integration.blackduck.imageinspector.image.oci.OciLayoutParser;
import com.blackduck.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.linux.Os;
import com.blackduck.integration.exception.IntegrationException;

@Component
public class ImageInspectorApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Gson gson;
    private ImageInspector imageInspector;
    private Os os;
    private FileOperations fileOperations;
    private BdioGenerator bdioGenerator;
    private PkgMgrExecutor pkgMgrExecutor;
    private CmdExecutor cmdExecutor;

    public ImageInspectorApi(ImageInspector imageInspector, Os os) {
        this.imageInspector = imageInspector;
        this.os = os;
        this.gson = new Gson();
    }

    @Autowired
    public void setBdioGenerator(final BdioGenerator bdioGenerator) {
        this.bdioGenerator = bdioGenerator;
    }

    // autowired does not work on Gson; not sure why
    public void setGson(final Gson gson) {
        this.gson = gson;
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
        if (gson == null) {
            gson = new Gson();
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

        List<ImageDirectoryDataExtractorFactory> imageDirectoryDataExtractorFactories = Arrays.asList(
            new OciImageDirectoryDataExtractorFactory(new OciImageFormatMatchesChecker(new OciLayoutParser(gson)), new CommonImageConfigParser(gson), gson),
            new DockerImageDirectoryDataExtractorFactory(new DockerImageFormatMatchesChecker(), new CommonImageConfigParser(gson), gson)
        );
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
