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

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ComponentHierarchyBuilder;
import com.synopsys.integration.blackduck.imageinspector.lib.*;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;
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
     * @param imageTarfilePath                          Required. The path to the Docker or OCI image tarfile (produced, for example, using the "docker save" command).
     * @param blackDuckProjectName                      Optional. The Black Duck project name.
     * @param blackDuckProjectVersion                   Optional. The Black Duck project version.
     * @param codeLocationPrefix                        Optional. A String to be pre-pended to the generated code location name.
     * @param givenImageRepo                            Optional. The image repo name. Required only if the given tarfile contains multiple images.
     * @param givenImageTag                             Optional. The image repo tag.  Required only if the given tarfile contains multiple images.
     * @param organizeComponentsByLayer                 If true, includes in BDIO image layers (and components found after layer applied). Set to false for original behavior.
     * @param includeRemovedComponents                  If true, includes in BDIO components found in lower layers that are not present in final container files system. Set to false for original behavior.
     * @param cleanupWorkingDir                         If false, files will be left behind that might be useful for troubleshooting. Should usually be set to true.
     * @param containerFileSystemOutputPath             Optional. The path to which the re-constructed container filesystem will be written as a .tar.gz file.
     * @param containerFileSystemExcludedPathListString Optional. A comma-separated list of glob patterns for directories to omit from the generated containerFileSystem file.
     * @param currentLinuxDistro                        Optional. The name of the Linux distro (from the ID field of /etc/os-release or /etc/lsb-release) of the machine on which this code is running.
     * @param targetLinuxDistroOverride                 Optional. The linux distro name to use when constructing BDIO. Used to override the name in the image with something equivalent that the Black Duck KB recognizes.
     * @param platformTopLayerExternalId                Optional. (Ignored if either organizeComponentsByLayer or includeRemovedComponents is true.) If you want to ignore components from the underlying platform, set this to the ID of the top layer of the platform. Components from the platform layers will be excluded from the output.
     * @return The generated BDIO object representing the componets (packages) read from the images's package manager database.
     * @throws IntegrationException, InterruptedException
     * @deprecated (use getBdio ( final ImageInspectionRequest imageInspectionRequest) instead)
     * Get a BDIO object representing the packages found in the image in the given tarfile. If the tarfile contains
     * more than one image, givenImageRepo and givenImageTag are used to select an image. If containerFileSystemOutputPath
     * is provided, this method will also write the container filesystem (reconstructed as part of the processing
     * required to read the image's packages) to that file as a .tar.gz file.
     */
    // TODO REMOVE and make this a major version
//    @Deprecated
//    public SimpleBdioDocument getBdio(
//        final String imageTarfilePath,
//        final String blackDuckProjectName,
//        final String blackDuckProjectVersion,
//        final String codeLocationPrefix,
//        final String givenImageRepo,
//        final String givenImageTag,
//        final boolean organizeComponentsByLayer,
//        final boolean includeRemovedComponents,
//        final boolean cleanupWorkingDir,
//        final String containerFileSystemOutputPath,
//        final String containerFileSystemExcludedPathListString,
//        final String currentLinuxDistro,
//        final String targetLinuxDistroOverride,
//        final String platformTopLayerExternalId)
//        throws IntegrationException, InterruptedException {
//
//        final ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
//                                                                  .setDockerTarfilePath(dockerTarfilePath)
//                                                                  .setBlackDuckProjectName(blackDuckProjectName)
//                                                                  .setBlackDuckProjectVersion(blackDuckProjectVersion)
//                                                                  .setCodeLocationPrefix(codeLocationPrefix)
//                                                                  .setGivenImageRepo(givenImageRepo)
//                                                                  .setGivenImageTag(givenImageTag)
//                                                                  .setOrganizeComponentsByLayer(organizeComponentsByLayer)
//                                                                  .setIncludeRemovedComponents(includeRemovedComponents)
//                                                                  .setCleanupWorkingDir(cleanupWorkingDir)
//                                                                  .setContainerFileSystemOutputPath(containerFileSystemOutputPath)
//                                                                  .setContainerFileSystemExcludedPathListString(containerFileSystemExcludedPathListString)
//                                                                  .setCurrentLinuxDistro(currentLinuxDistro)
//                                                                  .setTargetLinuxDistroOverride(targetLinuxDistroOverride)
//                                                                  .setPlatformTopLayerExternalId(platformTopLayerExternalId)
//                                                                  .build();
//        return getBdio(imageInspectionRequest);
//    }

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
        PackageGetter packageGetter = new PackageGetter(pkgMgrExecutor, cmdExecutor);
        ComponentHierarchyBuilder componentHierarchyBuilder = new ComponentHierarchyBuilder(packageGetter);
        return getBdio(componentHierarchyBuilder, imageInspectionRequest);
    }

    // TODO javadoc for this method
    public SimpleBdioDocument getBdio(ComponentHierarchyBuilder componentHierarchyBuilder, ImageInspectionRequest imageInspectionRequest) throws IntegrationException, InterruptedException {
        logger.info("getBdio()");
        os.logMemory();
        if (gsonBuilder == null) {
            gsonBuilder = new GsonBuilder();
        }
        return getBdioDocument(componentHierarchyBuilder, imageInspectionRequest);
    }

    private SimpleBdioDocument getBdioDocument(ComponentHierarchyBuilder componentHierarchyBuilder, ImageInspectionRequest imageInspectionRequest)
        throws IntegrationException, InterruptedException {
        final ImageInfoDerived imageInfoDerived = inspect(componentHierarchyBuilder, imageInspectionRequest);
        return imageInfoDerived.getBdioDocument();
    }

    private ImageInfoDerived inspect(ComponentHierarchyBuilder componentHierarchyBuilder, final ImageInspectionRequest imageInspectionRequest)
        throws IntegrationException, InterruptedException {
        final String effectivePlatformTopLayerExternalId;
        if (imageInspectionRequest.isOrganizeComponentsByLayer() || imageInspectionRequest.isIncludeRemovedComponents()) {
            // base image component exclusion not supported when either of these is true
            effectivePlatformTopLayerExternalId = null;
        } else {
            effectivePlatformTopLayerExternalId = imageInspectionRequest.getPlatformTopLayerExternalId();
        }

        File tempDir;
        try {
            tempDir = fileOperations.createTempDirectory();
        } catch (final IOException e) {
            throw new IntegrationException(String.format("Error creating temp dir: %s", e.getMessage()), e);
        }
        ImageInfoDerived imageInfoDerived = null;
        try {
            imageInfoDerived = imageInspector.inspectUsingGivenWorkingDir(componentHierarchyBuilder, imageInspectionRequest,
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
        return imageInfoDerived;
    }
}
