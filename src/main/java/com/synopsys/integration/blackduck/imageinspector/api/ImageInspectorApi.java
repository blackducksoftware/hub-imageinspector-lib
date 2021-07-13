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
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.TargetImageFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
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

    /**
     * @param dockerTarfilePath                         Required. The path to the docker image tarfile (produced using the "docker save" command).
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
     * Get a BDIO object representing the packages found in the docker image in the given tarfile. If the tarfile contains
     * more than one image, givenImageRepo and givenImageTag are used to select an image. If containerFileSystemOutputPath
     * is provided, this method will also write the container filesystem (reconstructed as part of the processing
     * required to read the image's packages) to that file as a .tar.gz file.
     */
    @Deprecated
    public SimpleBdioDocument getBdio(
        final String dockerTarfilePath,
        final String blackDuckProjectName,
        final String blackDuckProjectVersion,
        final String codeLocationPrefix,
        final String givenImageRepo,
        final String givenImageTag,
        final boolean organizeComponentsByLayer,
        final boolean includeRemovedComponents,
        final boolean cleanupWorkingDir,
        final String containerFileSystemOutputPath,
        final String containerFileSystemExcludedPathListString,
        final String currentLinuxDistro,
        final String targetLinuxDistroOverride,
        final String platformTopLayerExternalId)
        throws IntegrationException, InterruptedException {

        final ImageInspectionRequest imageInspectionRequest = (new ImageInspectionRequestBuilder())
                                                                  .setDockerTarfilePath(dockerTarfilePath)
                                                                  .setBlackDuckProjectName(blackDuckProjectName)
                                                                  .setBlackDuckProjectVersion(blackDuckProjectVersion)
                                                                  .setCodeLocationPrefix(codeLocationPrefix)
                                                                  .setGivenImageRepo(givenImageRepo)
                                                                  .setGivenImageTag(givenImageTag)
                                                                  .setOrganizeComponentsByLayer(organizeComponentsByLayer)
                                                                  .setIncludeRemovedComponents(includeRemovedComponents)
                                                                  .setCleanupWorkingDir(cleanupWorkingDir)
                                                                  .setContainerFileSystemOutputPath(containerFileSystemOutputPath)
                                                                  .setContainerFileSystemExcludedPathListString(containerFileSystemExcludedPathListString)
                                                                  .setCurrentLinuxDistro(currentLinuxDistro)
                                                                  .setTargetLinuxDistroOverride(targetLinuxDistroOverride)
                                                                  .setPlatformTopLayerExternalId(platformTopLayerExternalId)
                                                                  .build();
        return getBdio(imageInspectionRequest);
    }

    /**
     * Get a BDIO object representing the packages found in the docker image in the given tarfile. If the tarfile contains
     * more than one image, givenImageRepo and givenImageTag are used to select an image. If containerFileSystemOutputPath
     * is provided, this method will also write the container filesystem (reconstructed as part of the processing
     * required to read the image's packages) to that file as a .tar.gz file.
     * @param imageInspectionRequest Required. The request details.
     * @return The generated BDIO object representing the componets (packages) read from the images's package manager database.
     * @throws IntegrationException, InterruptedException
     */
    public SimpleBdioDocument getBdio(final ImageInspectionRequest imageInspectionRequest) throws IntegrationException, InterruptedException {
        logger.info("getBdio()");
        os.logMemory();
        if (gsonBuilder == null) {
            gsonBuilder = new GsonBuilder();
        }
        return getBdioDocument(imageInspectionRequest);
    }

    private SimpleBdioDocument getBdioDocument(final ImageInspectionRequest imageInspectionRequest)
        throws IntegrationException, InterruptedException {
        final ImageInfoDerived imageInfoDerived = inspect(imageInspectionRequest);
        return imageInfoDerived.getBdioDocument();
    }

    private ImageInfoDerived inspect(final ImageInspectionRequest imageInspectionRequest)
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
            imageInfoDerived = inspectUsingGivenWorkingDir(imageInspectionRequest,
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

    private ImageInfoDerived inspectUsingGivenWorkingDir(final ImageInspectionRequest imageInspectionRequest,
        final File tempDir,
        final String effectivePlatformTopLayerExternalId)
        throws IOException, IntegrationException {

        final File workingDir = new File(tempDir, "working");
        final File tarExtractionBaseDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        logger.debug(String.format("imageInspector: %s; workingDir: %s", imageInspector, workingDir.getAbsolutePath()));
        final File dockerTarfile = new File(imageInspectionRequest.getDockerTarfilePath());
        File tarExtractionDirectory = new File(tarExtractionBaseDirectory, dockerTarfile.getName());
        final File extractionDir = imageInspector.extractImageTar(tarExtractionDirectory, dockerTarfile);
        final List<TypedArchiveFile> layerTars = imageInspector.getLayerArchives(extractionDir);
        final ManifestLayerMapping manifestLayerMapping = imageInspector.getLayerMapping(gsonBuilder, tarExtractionDirectory, imageInspectionRequest.getGivenImageRepo(), imageInspectionRequest.getGivenImageTag());
        final ImageComponentHierarchy imageComponentHierarchy = imageInspector.createInitialImageComponentHierarchy(tarExtractionDirectory, manifestLayerMapping);
        final String imageRepo = manifestLayerMapping.getImageName();
        final String imageTag = manifestLayerMapping.getTagName();

        final File targetImageFileSystemParentDir = new File(tarExtractionBaseDirectory, ImageInspector.TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        File targetImageFileSystemAppLayersRootDir = null;
        if (StringUtils.isNotBlank(effectivePlatformTopLayerExternalId)) {
            targetImageFileSystemAppLayersRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemAppLayersRootDirName(imageRepo, imageTag));
        }
        final TargetImageFileSystem targetImageFileSystem = new TargetImageFileSystem(targetImageFileSystemRootDir, targetImageFileSystemAppLayersRootDir);
        final ImageInspectorOsEnum currentOs = os.deriveOs(imageInspectionRequest.getCurrentLinuxDistro());
        final ImageInfoParsed imageInfoParsed = imageInspector
                                                    .extractDockerLayers(gsonBuilder, currentOs, imageInspectionRequest.getTargetLinuxDistroOverride(), imageComponentHierarchy, targetImageFileSystem, layerTars, manifestLayerMapping,
                                                        imageInspectionRequest.getPlatformTopLayerExternalId());
        validatePlatformResults(effectivePlatformTopLayerExternalId, imageComponentHierarchy);
        logLayers(imageComponentHierarchy);
        cleanUpLayerTars(imageInspectionRequest.isCleanupWorkingDir(), layerTars);
        ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromGivenComponents(bdioGenerator, imageInfoParsed, imageComponentHierarchy, manifestLayerMapping,
            imageInspectionRequest.getBlackDuckProjectName(), imageInspectionRequest.getBlackDuckProjectVersion(),
            imageInspectionRequest.getCodeLocationPrefix(), imageInspectionRequest.isOrganizeComponentsByLayer(), imageInspectionRequest.isIncludeRemovedComponents(),
            StringUtils.isNotBlank(effectivePlatformTopLayerExternalId));
        createContainerFileSystemTarIfRequested(targetImageFileSystem, imageInspectionRequest.getContainerFileSystemOutputPath(),
            imageInspectionRequest.getContainerFileSystemExcludedPathListString());
        return imageInfoDerived;
    }

    private void validatePlatformResults(final String givenPlatformTopLayerExternalId, final ImageComponentHierarchy imageComponentHierarchy) throws IntegrationException {
        if ((StringUtils.isNotBlank(givenPlatformTopLayerExternalId)) && (!imageComponentHierarchy.isPlatformTopLayerFound())) {
            throw new IntegrationException(String.format("Platform top layer id (%s) was specified but not found", givenPlatformTopLayerExternalId));
        }
    }

    private void logLayers(final ImageComponentHierarchy imageComponentHierarchy) {
        if (!logger.isTraceEnabled()) {
            return;
        }
        logger.trace("layer dump:");
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            if (layer == null) {
                logger.trace("Layer is null");
            } else if (layer.getComponents() == null) {
                logger.trace(String.format("layer %s has no componenents", layer.getLayerIndexedName()));
            } else {
                logger.trace(String.format("Layer %s has %d components; layer cmd: %s", layer.getLayerIndexedName(), layer.getComponents().size(), layer.getLayerCmd()));
            }
        }
        if (imageComponentHierarchy.getFinalComponents() == null) {
            logger.trace("Final image components list not set");
        } else {
            logger.trace(String.format("Final image components list has %d components", imageComponentHierarchy.getFinalComponents().size()));
        }
    }

    private void cleanUpLayerTars(final boolean cleanupWorkingDir, final List<TypedArchiveFile> layerTars) {
        if (cleanupWorkingDir) {
            for (final TypedArchiveFile layerTar : layerTars) {
                logger.trace(String.format("Deleting %s", layerTar.getFile().getAbsolutePath()));
                fileOperations.deleteQuietly(layerTar.getFile());
            }
        }
    }

    private void createContainerFileSystemTarIfRequested(final TargetImageFileSystem targetImageFileSystem, final String containerFileSystemOutputPath, final String containerFileSystemExcludedPathListString) {
        if (StringUtils.isNotBlank(containerFileSystemOutputPath)) {
            logger.info("Including container file system in output");
            final File outputDirectory = new File(containerFileSystemOutputPath);
            final File containerFileSystemTarFile = new File(containerFileSystemOutputPath);
            final File returnedTargetImageFileSystem = targetImageFileSystem.getTargetImageFileSystemAppOnly().orElse(targetImageFileSystem.getTargetImageFileSystemFull());
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), returnedTargetImageFileSystem.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final LinuxFileSystem containerFileSys = new LinuxFileSystem(returnedTargetImageFileSystem, fileOperations);
            containerFileSys.writeToTarGz(containerFileSystemTarFile, containerFileSystemExcludedPathListString);
        }
    }

}
