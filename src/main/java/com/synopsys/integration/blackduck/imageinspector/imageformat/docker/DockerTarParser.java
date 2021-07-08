/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.imageinspector.imageformat.common.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.imageformat.common.TypedArchiveFile;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.PackageManagerToImageInspectorOsMapping;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.Manifest;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.lib.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.TargetImageFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class DockerTarParser {
    private static final String DOCKER_LAYER_TAR_FILENAME = "layer.tar";
    private static final String DOCKER_LAYER_METADATA_FILENAME = "json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private CmdExecutor executor;
    private ManifestFactory manifestFactory;
    private Os os;
    private ImageConfigParser imageConfigParser;
    private LayerConfigParser layerConfigParser;
    private FileOperations fileOperations;
    private List<PkgMgr> pkgMgrs;
    private PkgMgrExecutor pkgMgrExecutor;
    private DockerLayerTarExtractor dockerLayerTarExtractor;

    @Autowired
    public void setExecutor(final CmdExecutor executor) {
        this.executor = executor;
    }
    @Autowired
    public void setPkgMgrExecutor(final PkgMgrExecutor pkgMgrExecutor) {
        this.pkgMgrExecutor = pkgMgrExecutor;
    }

    @Autowired
    public void setPkgMgrs(final List<PkgMgr> pkgMgrs) {
        this.pkgMgrs = pkgMgrs;
    }

    @Autowired
    public void setOs(final Os os) {
        this.os = os;
    }

    @Autowired
    public void setManifestFactory(final ManifestFactory manifestFactory) {
        this.manifestFactory = manifestFactory;
    }

    @Autowired
    public void setImageConfigParser(final ImageConfigParser imageConfigParser) {
        this.imageConfigParser = imageConfigParser;
    }

    @Autowired
    public void setLayerConfigParser(final LayerConfigParser layerConfigParser) {
        this.layerConfigParser = layerConfigParser;
    }

    @Autowired
    public void setFileOperations(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    @Autowired
    public void setDockerLayerTarExtractor(final DockerLayerTarExtractor dockerLayerTarExtractor) {
        this.dockerLayerTarExtractor = dockerLayerTarExtractor;
    }

    public List<TypedArchiveFile> getLayerArchives(final File unpackedImageDir) throws IOException {
        logger.debug(String.format("Searching for layer archive files in unpackedImageDir: %s", unpackedImageDir.getAbsolutePath()));
        final List<TypedArchiveFile> untaredLayerFiles = new ArrayList<>();
        List<File> unpackedImageTopLevelFiles = Arrays.asList(unpackedImageDir.listFiles());
        for (File unpackedImageTopLevelFile : unpackedImageTopLevelFiles) {
            if (unpackedImageTopLevelFile.isDirectory()) {
                List<File> unpackedImageSecondLevelFiles = Arrays.asList(unpackedImageTopLevelFile.listFiles());
                for (File unpackedImageSecondLevelFile : unpackedImageSecondLevelFiles) {
                    if (unpackedImageSecondLevelFile.isFile() && unpackedImageSecondLevelFile.getName().equals(DOCKER_LAYER_TAR_FILENAME)) {
                        TypedArchiveFile typedArchiveFile = new TypedArchiveFile(ArchiveFileType.TAR, unpackedImageSecondLevelFile);
                        untaredLayerFiles.add(typedArchiveFile);
                    }
                }
            }
        }
        return untaredLayerFiles;
    }

    public ManifestLayerMapping getLayerMapping(final GsonBuilder gsonBuilder, final File tarExtractionBaseDirectory, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        logger.debug(String.format("getLayerMappings(): dockerImageName: %s; dockerTagName: %s", dockerImageName, dockerTagName));
        logger.debug(String.format("tarExtractionBaseDirectory: %s", tarExtractionBaseDirectory));
        final File tarExtractionSubDirectory = new File(tarExtractionBaseDirectory, tarFileName);
        final Manifest manifest = manifestFactory.createManifest(tarExtractionBaseDirectory, tarFileName);
        ManifestLayerMapping partialMapping;
        try {
            partialMapping = manifest.getLayerMapping(dockerImageName, dockerTagName);
        } catch (final Exception e) {
            final String msg = String.format("Could not parse the image manifest file : %s", e.getMessage());
            logger.error(msg);
            throw new IntegrationException(msg, e);
        }
        final List<String> externalLayerIds = getExternalLayerIdsFromImageConfigFile(gsonBuilder, tarExtractionSubDirectory, partialMapping.getImageConfigFilename());
        if (externalLayerIds.isEmpty()) {
            return partialMapping;
        }
        return new ManifestLayerMapping(partialMapping, externalLayerIds);
    }

    public ImageComponentHierarchy createInitialImageComponentHierarchy(final File tarExtractionDirectory, final String tarFileName, final ManifestLayerMapping manifestLayerMapping) throws IntegrationException {
        String manifestFileContents = null;
        String configFileContents = null;
        File tarContentsDirectory = new File(tarExtractionDirectory, tarFileName);
        for (File tarFileContentsFile : fileOperations.listFilesInDir(tarContentsDirectory)) {
            logger.trace(String.format("File %s", tarFileContentsFile.getName()));
            if ("manifest.json".equals(tarFileContentsFile.getName())) {
                try {
                    manifestFileContents = FileUtils.readFileToString(tarFileContentsFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IntegrationException(String.format("Error reading manifest file %s", tarFileContentsFile.getAbsolutePath()));
                }
            } else if (tarFileContentsFile.getName().equals(manifestLayerMapping.getImageConfigFilename())) {
                try {
                    configFileContents = FileUtils.readFileToString(tarFileContentsFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IntegrationException(String.format("Error reading config file %s", tarFileContentsFile.getAbsolutePath()));
                }
            }
        }
        return new ImageComponentHierarchy(manifestFileContents, configFileContents);
    }

    public ImageInfoParsed extractImageLayers(final GsonBuilder gsonBuilder, final ImageInspectorOsEnum currentOs, final String targetLinuxDistroOverride, final ImageComponentHierarchy imageComponentHierarchy,
        final TargetImageFileSystem targetImageFileSystem, final List<TypedArchiveFile> layerTars, final ManifestLayerMapping manifestLayerMapping,
        final String platformTopLayerExternalId) throws IOException, WrongInspectorOsException {
        ImageInfoParsed imageInfoParsed = null;
        int layerIndex = 0;
        boolean inApplicationLayers = false;
        for (final String layerDotTarDirname : manifestLayerMapping.getLayerInternalIds()) {
            logger.trace(String.format("Looking for tar for layer: %s", layerDotTarDirname));
            final TypedArchiveFile layerTar = getLayerTar(layerTars, layerDotTarDirname);
            if (layerTar != null) {
                extractLayerTarToDir(targetImageFileSystem.getTargetImageFileSystemFull(), layerTar);
                if (inApplicationLayers && targetImageFileSystem.getTargetImageFileSystemAppOnly().isPresent()) {
                    extractLayerTarToDir(targetImageFileSystem.getTargetImageFileSystemAppOnly().get(), layerTar);
                }
                final String layerMetadataFileContents = getLayerMetadataFileContents(layerTar);
                final List<String> layerCmd = layerConfigParser.parseCmd(gsonBuilder, layerMetadataFileContents);
                final boolean isPlatformTopLayer = isThisThePlatformTopLayer(manifestLayerMapping, platformTopLayerExternalId, layerIndex);
                if (isPlatformTopLayer) {
                    imageComponentHierarchy.setPlatformTopLayerIndex(layerIndex);
                    inApplicationLayers = true; // will be true next iteration
                    logger.info(String.format("Layer %d is the top layer of the platform. Components present after adding this layer will be omitted from results", layerIndex));
                }
                imageInfoParsed = addPostLayerComponents(layerIndex, currentOs, targetLinuxDistroOverride, imageInfoParsed, imageComponentHierarchy, targetImageFileSystem, layerMetadataFileContents, layerCmd,
                    manifestLayerMapping.getLayerExternalId(layerIndex), isPlatformTopLayer);
            } else {
                logger.error(String.format("Could not find the tar for layer %s", layerDotTarDirname));
            }
            layerIndex++;
        }
        List<LayerDetails> layers = imageComponentHierarchy.getLayers();
        int numLayers = layers.size();
        if (numLayers > 0) {
            LayerDetails topLayer = layers.get(numLayers - 1);
            final List<ComponentDetails> netComponents = getNetComponents(topLayer.getComponents(), imageComponentHierarchy.getPlatformComponents());
            imageComponentHierarchy.setFinalComponents(netComponents);
        }
        if (imageInfoParsed == null) {
            imageInfoParsed = new ImageInfoParsed(targetImageFileSystem, new ImagePkgMgrDatabase(null, PackageManagerEnum.NULL), targetLinuxDistroOverride, null);
        }
        return imageInfoParsed;
    }

    private boolean isThisThePlatformTopLayer(final ManifestLayerMapping manifestLayerMapping, final String platformTopLayerExternalId, final int layerIndex) {
        final String currentLayerExternalId = manifestLayerMapping.getLayerExternalId(layerIndex);
        boolean isTop = (platformTopLayerExternalId != null) && platformTopLayerExternalId.equals(currentLayerExternalId);
        logger.trace(String.format("Results of test for top of platform: layerIndex: %d, platformTopLayerExternalId: %s, currentLayerExternalId: %s, isTop: %b", layerIndex, platformTopLayerExternalId, currentLayerExternalId, isTop));
        return isTop;
    }

    private List<ComponentDetails> getNetComponents(final List<ComponentDetails> grossComponents, final List<ComponentDetails> componentsToOmit) {
        logger.info(String.format("There are %d components to omit", componentsToOmit.size()));
        if (componentsToOmit.isEmpty()) {
            return grossComponents;
        }
        List<ComponentDetails> netComponents = ListUtils.subtract(grossComponents, componentsToOmit);
        logger.debug(String.format("grossComponents: %d, componentsToOmit: %d, netComponents: %d", grossComponents.size(), componentsToOmit.size(), netComponents.size()));
        return netComponents;
    }

    private List<String> getExternalLayerIdsFromImageConfigFile(final GsonBuilder gsonBuilder, final File tarExtractionDirectory, final String imageConfigFileName) {
        try {
            final File imageConfigFile = new File(tarExtractionDirectory, imageConfigFileName);
            final String imageConfigFileContents = fileOperations
                                                       .readFileToString(imageConfigFile);
            logger.trace(String.format("imageConfigFileContents (%s): %s", imageConfigFile.getName(), imageConfigFileContents));
            return imageConfigParser.parseExternalLayerIds(gsonBuilder, imageConfigFileContents);
        } catch (Exception e) {
            logger.warn(String.format("Error logging image config file contents: %s", e.getMessage()));
        }
        return new ArrayList<>(0);
    }

    private Optional<String> extractLinuxDistroNameFromFileSystem(final File targetImageFileSystemRootDir) {
        final LinuxFileSystem extractedFileSys = new LinuxFileSystem(targetImageFileSystemRootDir, fileOperations);
        final Optional<File> etcDir = extractedFileSys.getEtcDir();
        if (!etcDir.isPresent()) {
            return Optional.empty();
        }
        return extractLinuxDistroNameFromEtcDir(etcDir.get());
    }

    Optional<String> extractLinuxDistroNameFromEtcDir(final File etcDir) {
        logger.trace(String.format("/etc directory: %s", etcDir.getAbsolutePath()));
        if (fileOperations.listFilesInDir(etcDir).length == 0) {
            logger.warn(String.format("Could not determine the Operating System because the /etc dir (%s) is empty", etcDir.getAbsolutePath()));
        }
        return os.getLinuxDistroNameFromEtcDir(etcDir);
    }

    private void extractLayerTarToDir(final File destinationDir, final TypedArchiveFile layerTar) throws IOException {
        logger.trace(String.format("Extracting layer: %s into %s", layerTar.getFile().getAbsolutePath(), destinationDir.getAbsolutePath()));
        final List<File> filesToRemove = dockerLayerTarExtractor.extractLayerTarToDir(fileOperations, layerTar.getFile(), destinationDir);
        for (final File fileToRemove : filesToRemove) {
            if (fileToRemove.isDirectory()) {
                logger.trace(String.format("Removing dir marked for deletion: %s", fileToRemove.getAbsolutePath()));
                FileUtils.deleteDirectory(fileToRemove);
            } else {
                logger.trace(String.format("Removing file marked for deletion: %s", fileToRemove.getAbsolutePath()));
                fileOperations.deleteQuietly(fileToRemove);
            }
        }
    }

    private TypedArchiveFile getLayerTar(final List<TypedArchiveFile> layerTars, final String layer) {
        TypedArchiveFile layerTar = null;
        for (final TypedArchiveFile candidateLayerTar : layerTars) {
            if (layer.equals(candidateLayerTar.getFile().getParentFile().getName())) {
                logger.trace(String.format("Found layer tar for layer %s", layer));
                layerTar = candidateLayerTar;
                break;
            }
        }
        return layerTar;
    }

    private String getLayerMetadataFileContents(final TypedArchiveFile layerTarFile) {
        String layerMetadataFileContents = null;
        File dir = layerTarFile.getFile().getParentFile();
        File metadataFile = new File(dir, DOCKER_LAYER_METADATA_FILENAME);
        try {
            if (metadataFile.exists()) {
                layerMetadataFileContents = FileUtils.readFileToString(metadataFile, StandardCharsets.UTF_8);
                logger.trace(String.format("%s: %s", metadataFile.getAbsolutePath(), layerMetadataFileContents));
            }
        } catch (IOException e) {
            logger.trace(String.format("Unable to log contents of %s: %s", metadataFile.getAbsolutePath(), e.getMessage()));
        }
        return layerMetadataFileContents;
    }

    private ImageInfoParsed addPostLayerComponents(final int layerIndex, final ImageInspectorOsEnum currentOs, final String targetLinuxDistroOverride, ImageInfoParsed imageInfoParsed,
        final ImageComponentHierarchy imageComponentHierarchy, final TargetImageFileSystem targetImageFileSystem, final String layerMetadataFileContents,
        final List<String> layerCmd, final String layerExternalId,
        boolean isPlatformTopLayer) throws WrongInspectorOsException {
        logger.debug(String.format("Getting components present (so far) after adding layer %d", layerIndex));
        logger.trace(String.format("Layer ID: %s", layerExternalId));
        if (currentOs == null) {
            logger.debug(String.format("Current (running on) OS not provided; cannot determine components present after adding layer %d", layerIndex));
            return null;
        }
        try {
            if (imageInfoParsed == null) {
                logger.debug("Attempting to determine the target image package manager");
                imageInfoParsed = parseImageInfo(targetImageFileSystem, targetLinuxDistroOverride);
                final ImageInspectorOsEnum neededInspectorOs = PackageManagerToImageInspectorOsMapping
                                        .getImageInspectorOs(imageInfoParsed.getImagePkgMgrDatabase().getPackageManager());
                if (!neededInspectorOs.equals(currentOs)) {
                    final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs.toString());
                    throw new WrongInspectorOsException(neededInspectorOs, msg);
                }
            } else {
                logger.debug(String.format("The target image package manager has previously been determined: %s", imageInfoParsed.getImagePkgMgrDatabase().getPackageManager().toString()));
            }
            final List<ComponentDetails> comps = queryPkgMgrForDependencies(imageInfoParsed, layerIndex);
            if (comps.isEmpty()) {
                return imageInfoParsed;
            }
            logger.info(String.format("Found %d components in file system after adding layer %d", comps.size(), layerIndex));
            for (ComponentDetails comp : comps) {
                logger.trace(String.format("\t%s/%s/%s", comp.getName(), comp.getVersion(), comp.getArchitecture()));
            }
            final LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd, comps);
            imageComponentHierarchy.addLayer(layer);
            if (isPlatformTopLayer) {
                imageComponentHierarchy.setPlatformComponents(comps);
            }
        } catch (final WrongInspectorOsException wrongOsException) {
            throw wrongOsException;
        } catch (final PkgMgrDataNotFoundException pkgMgrDataNotFoundException) {
            logger.debug(String.format("Unable to collect components present after layer %d: The file system is not yet populated with the linux distro and package manager files: %s", layerIndex, pkgMgrDataNotFoundException.getMessage()));
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd,  null);
            imageComponentHierarchy.addLayer(layer);
        } catch (final Exception otherException) {
            logger.debug(String.format("Unable to collect components present after layer %d", layerIndex));
            LayerDetails layer = new LayerDetails(layerIndex, layerExternalId, layerMetadataFileContents, layerCmd,  null);
            imageComponentHierarchy.addLayer(layer);
        }
        return imageInfoParsed;
    }

    ImageInfoParsed parseImageInfo(final TargetImageFileSystem targetImageFileSystem, final String targetLinuxDistroOverride) throws PkgMgrDataNotFoundException {
        if (pkgMgrs == null) {
            logger.error("No pmgMgrs configured");
        } else {
            logger.trace(String.format("pkgMgrs.size(): %d", pkgMgrs.size()));
            for (PkgMgr pkgMgr : pkgMgrs) {
                if (pkgMgr.isApplicable(targetImageFileSystem.getTargetImageFileSystemFull())) {
                    logger.trace(String.format("Package manager %s applies", pkgMgr.getType().toString()));
                    final ImagePkgMgrDatabase targetImagePkgMgr = new ImagePkgMgrDatabase(pkgMgr.getImagePackageManagerDirectory(targetImageFileSystem.getTargetImageFileSystemFull()),
                        pkgMgr.getType());
                    final String linuxDistroName;
                    if (StringUtils.isNotBlank(targetLinuxDistroOverride)) {
                        linuxDistroName = targetLinuxDistroOverride;
                        logger.trace(String.format("Target linux distro name overridden by caller to: %s", linuxDistroName));
                    } else {
                        linuxDistroName = extractLinuxDistroNameFromFileSystem(targetImageFileSystem.getTargetImageFileSystemFull()).orElse(null);
                        logger.trace(String.format("Target linux distro name derived from image file system: %s", linuxDistroName));
                    }
                    return new ImageInfoParsed(targetImageFileSystem, targetImagePkgMgr, linuxDistroName, pkgMgr);
                }
            }
        }
        throw new PkgMgrDataNotFoundException("No package manager database found in this Docker image.");
    }

    private List<ComponentDetails> queryPkgMgrForDependencies(final ImageInfoParsed imageInfoParsed, final int layerIndex) {
        final List<ComponentDetails> comps;
        try {
            final String[] pkgMgrOutputLines = pkgMgrExecutor.runPackageManager(executor, imageInfoParsed.getPkgMgr(), imageInfoParsed.getImagePkgMgrDatabase());
            comps = imageInfoParsed.getPkgMgr().extractComponentsFromPkgMgrOutput(imageInfoParsed.getTargetImageFileSystem().getTargetImageFileSystemFull(), imageInfoParsed.getLinuxDistroName(), pkgMgrOutputLines);
        } catch (IntegrationException e) {
            logger.debug(String.format("Unable to log components present after layer %d: %s", layerIndex, e.getMessage()));
            return new ArrayList<>(0);
        }
        logger.info(String.format("Found %d components in file system after adding layer %d", comps.size(), layerIndex));
        return comps;
    }
}
