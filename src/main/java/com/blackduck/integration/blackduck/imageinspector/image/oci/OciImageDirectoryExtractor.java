/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2025 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.blackduck.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import com.blackduck.integration.blackduck.imageinspector.image.common.*;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciDescriptor;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciImageManifest;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.exception.IntegrationException;

public class OciImageDirectoryExtractor implements ImageDirectoryExtractor {
    private static final String INDEX_FILE_NAME = "index.json";
    private static final String BLOBS_DIR_NAME = "blobs";
    private static final String CONFIG_FIELD_NAME = "\"Config\":\"";

    private static final String INDEX_FILE_MEDIA_TYPE = "application/vnd.oci.image.index.v1+json";
    private static final String CONFIG_FILE_MEDIA_TYPE = "application/vnd.oci.image.config.v1+json";

    private static final String LAYER_ARCHIVE_TAR_MEDIA_TYPE_SUFFIX = ".tar";
    private static final String LAYER_ARCHIVE_TAR_GZIP_MEDIA_TYPE_SUFFIX = ".tar+gzip";
    private static final String LAYER_ARCHIVE_TAR_ZSTD_MEDIA_TYPE_SUFFIX = ".tar+zstd";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Gson gson;
    private final FileOperations fileOperations;
    private final ImageNameResolver imageNameResolver;
    private final CommonImageConfigParser commonImageConfigParser;
    private final OciImageIndexFileParser ociImageIndexFileParser;
    private final OciManifestDescriptorParser ociManifestDescriptorParser;

    public OciImageDirectoryExtractor(final Gson gson, FileOperations fileOperations, ImageNameResolver imageNameResolver, CommonImageConfigParser commonImageConfigParser,
                                      OciImageIndexFileParser ociImageIndexFileParser, OciManifestDescriptorParser ociManifestDescriptorParser) {
        this.gson = gson;
        this.fileOperations = fileOperations;
        this.imageNameResolver = imageNameResolver;
        this.commonImageConfigParser = commonImageConfigParser;
        this.ociImageIndexFileParser = ociImageIndexFileParser;
        this.ociManifestDescriptorParser = ociManifestDescriptorParser;
    }

    @Override
    public List<TypedArchiveFile> getLayerArchives(final File imageDir, @Nullable String givenRepo, @Nullable String givenTag) throws IntegrationException {
        File blobsDir = new File(imageDir, BLOBS_DIR_NAME);

        // TODO this code is repeated below / executed twice
        OciImageIndex ociImageIndex = extractOciImageIndex(imageDir);
        OciDescriptor manifestDescriptor = ociManifestDescriptorParser.getManifestDescriptor(ociImageIndex, givenRepo, givenTag);
        File manifestFile = findManifestFile(imageDir, manifestDescriptor);

        try {
            return parseLayerArchives(manifestFile, blobsDir, imageDir);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error parsing layer archives from manifest file %s: %s", manifestFile.getAbsolutePath(), e.getMessage()), e);
        }
    }

    @Override
    public FullLayerMapping getLayerMapping(final File imageDir, @Nullable String givenRepo, @Nullable String givenTag) throws IntegrationException {
        OciImageIndex ociImageIndex = extractOciImageIndex(imageDir);
        OciDescriptor manifestDescriptor = ociManifestDescriptorParser.getManifestDescriptor(ociImageIndex, givenRepo, givenTag);
        logger.debug(String.format("foundRepoTag: %s", manifestDescriptor.getRepoTagString().orElse("")));

        String manifestRepoTag = manifestDescriptor.getRepoTagString().orElse(null);
        if (manifestRepoTag != null && !manifestRepoTag.contains(":") && givenRepo != null) {
            if (givenTag != null && givenTag.isBlank()) {
                givenTag = "latest";
            }
            manifestRepoTag = String.format("%s:%s", givenRepo, givenTag);
        }
        RepoTag resolvedRepoTag = imageNameResolver.resolve(manifestRepoTag, givenRepo, givenTag);

        logger.debug(String.format("Based on manifest, translated repoTag to: repo: %s, tag: %s", resolvedRepoTag.getRepo().orElse(""), resolvedRepoTag.getTag().orElse("")));
        File manifestFile = findManifestFile(imageDir, manifestDescriptor);
        String manifestFileText;
        try {
            logger.debug("Path to the manifest file about to be read: {}", manifestFile.toPath().toString());
            manifestFileText = fileOperations.readFileToString(manifestFile);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Unable to parse manifest file %s", manifestFile.getAbsolutePath()));
        }
        
        String pathToImageConfigFileFromRoot = null;
        List<String> layerInternalIds;
        
        List<String> layerExternalIds;
        OciImageManifest imageManifest = gson.fromJson(manifestFileText, OciImageManifest.class);
        if (imageManifest == null || imageManifest.getConfig() == null) {
            logger.debug("JSON text is not of Image Manifest type: {}", manifestFileText);
            OciImageIndex imageIndex = gson.fromJson(manifestFileText, OciImageIndex.class);
            if (imageIndex == null || imageIndex.getManifests() == null) {
                throw new IntegrationException("Unable to find a matching manifest with config file");
            }
            File rootManifestfile = new File(imageDir, "manifest.json");
            try {
                String rootManifestFileText = fileOperations.readFileToString(rootManifestfile);
                pathToImageConfigFileFromRoot = getConfigDigestFromRootManifestText(rootManifestFileText);
                logger.debug("configRelativePathFromRoot: \n{}", pathToImageConfigFileFromRoot);
                if (pathToImageConfigFileFromRoot == null) {
                    throw new IntegrationException("Unable to find config in root manifest.");
                }
            } catch (IOException ex) {
                throw new IntegrationException("Unable to find a matching manifest with config file in the root: {}", ex);
            }
            layerInternalIds = imageIndex.getManifests().stream()
                                            .map(OciDescriptor::getDigest)
                                            .collect(Collectors.toList());
        } else {
            // If we ever need more detail (os/architecture, history, cmd, etc):
            // imageManifest.config.digest has the filename (in the blobs dir) of the file that has that detail
            pathToImageConfigFileFromRoot = findImageConfigFilePath(imageManifest.getConfig());
            layerInternalIds = imageManifest.getLayers().stream()
                                            .map(OciDescriptor::getDigest)
                                            .collect(Collectors.toList());
        }
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(
                resolvedRepoTag.getRepo().orElse(""), 
                resolvedRepoTag.getTag().orElse(""), 
                pathToImageConfigFileFromRoot, 
                layerInternalIds);

        layerExternalIds = commonImageConfigParser.getExternalLayerIdsFromImageConfigFile(imageDir, pathToImageConfigFileFromRoot);
        return new FullLayerMapping(manifestLayerMapping, layerExternalIds);
    }

    private OciImageIndex extractOciImageIndex(File imageDir) throws IntegrationException {
        File indexFile = new File (imageDir, INDEX_FILE_NAME);
        return ociImageIndexFileParser.loadIndex(indexFile);
    }

    private File findManifestFile(File imageDir, OciDescriptor manifestDescriptor) throws IntegrationException {
        String manifestFileDigest = manifestDescriptor.getDigest();

        String pathToManifestFile = parsePathToBlobFileFromDigest(manifestFileDigest);
        logger.trace("Path to manifest file: {}", pathToManifestFile);
        File blobsDir = new File(imageDir, BLOBS_DIR_NAME);
        return findBlob(blobsDir, pathToManifestFile);
    }

    private ArchiveFileType parseArchiveTypeFromLayerDescriptorMediaType(String mediaType, String digest) throws IntegrationException {
        if (mediaType.contains("nondistributable")) {
            //TODO- what do we do with archives "nondistributable" media types? https://github.com/opencontainers/image-spec/blob/main/layer.md#non-distributable-layers
            // ac- based on the linked doc, I think we should just treat them normally (as if they were their "distributable" counterparts)
        }
        if (mediaType.endsWith(LAYER_ARCHIVE_TAR_MEDIA_TYPE_SUFFIX)) {
            return ArchiveFileType.TAR;
        } else if (mediaType.endsWith(LAYER_ARCHIVE_TAR_GZIP_MEDIA_TYPE_SUFFIX)) {
            return ArchiveFileType.TAR_GZIPPED;
        } else if (mediaType.endsWith(LAYER_ARCHIVE_TAR_ZSTD_MEDIA_TYPE_SUFFIX)) {
            return ArchiveFileType.TAR_ZSTD;
        } else {
            throw new IntegrationException(String.format("Possible unsupported input archive file type. Please refer to the relevant Docker Inspector documentation at https://documentation.blackduck.com/bundle/detect/page/packagemgrs/docker/formats.html. Unrecognized media type %s of layer %s.", mediaType, digest));
        }
    }

    private List<TypedArchiveFile> parseLayerArchives(File manifestFile, File blobsDir, File imageDir) throws IOException {
        
        // Parse manifest file for names + archive formats of layer files
        String manifestFileText = fileOperations.readFileToString(manifestFile);
        logger.debug("parseLayerArchives - manifestFileText: {}, blobsDir: {}, imageDir: {}", manifestFileText, blobsDir, imageDir);
        OciImageManifest imageManifest = gson.fromJson(manifestFileText, OciImageManifest.class);
        List<OciDescriptor> layersOrManifests;
        List<TypedArchiveFile> layerArchives = new LinkedList<>();
        if (imageManifest == null || imageManifest.getLayers() == null) {
            logger.debug("JSON text did not match Image Manifest: {}\n", manifestFileText);
            OciImageIndex imageIndex = gson.fromJson(manifestFileText, OciImageIndex.class);
            if (imageIndex == null || imageIndex.getManifests() == null) {
                logger.debug("JSON text did not match Image Index either.");
                return layerArchives;
            } else {
                layersOrManifests = imageIndex.getManifests();
            }
        } else {
            layersOrManifests = imageManifest.getLayers();
        }
        logger.debug("parseLayerArchives - layersOrManifests.size(): {}", layersOrManifests.size());
        for (OciDescriptor layer : layersOrManifests) {
            String pathToLayerFile = parsePathToBlobFileFromDigest(layer.getDigest());
            logger.debug("parseLayerArchives - pathToLayerFile: {}", pathToLayerFile);
            File layerFile;
            try {
                if (pathToLayerFile.startsWith(BLOBS_DIR_NAME)) {
                    logger.debug("imageDir: {}, pathToLayerFile: {}", imageDir, pathToLayerFile);
                    layerFile = findBlob(imageDir, pathToLayerFile);
                } else {
                    logger.debug("blobsDir: {}, pathToLayerFile: {}", blobsDir, pathToLayerFile);
                    layerFile = findBlob(blobsDir, pathToLayerFile);
                }
            } catch (IntegrationException e) {
                logger.error(e.getMessage());
                continue;
            }
            logger.debug("parseLayerArchives - layerFile: {}", layerFile);
            ArchiveFileType archiveFileType;
            try {
                logger.debug("parseLayerArchives - layer.getMediaType(): {}", layer.getMediaType());
                archiveFileType = parseArchiveTypeFromLayerDescriptorMediaType(layer.getMediaType(), layer.getDigest());
            } catch (IntegrationException e) {
                logger.error(e.getMessage());
                continue;
            }
            layerArchives.add(new TypedArchiveFile(archiveFileType, layerFile));
        }
        logger.debug("parseLayerArchives - layerArchives.size(): {}", layerArchives.size());
        return layerArchives;
    }

    // Digests are in the format <hash algorithm>:<hash of content> and the path to the file from "blobs" dir is <hash algorithm>/<hash of contents>
    private String parsePathToBlobFileFromDigest(String digest) {
        return String.join("/", digest.split(":"));
    }

    private File findBlob(File blobsDir, String pathToBlob) throws IntegrationException {
        File blob = new File(blobsDir, pathToBlob);
        if (!blob.exists()) {
            throw new IntegrationException(String.format("Blob referenced by image manifest could not be found at %s.", blob.getAbsolutePath()));
        }
        return blob;
    }

    private String findImageConfigFilePath(OciDescriptor imageConfig) throws IntegrationException {
        if (imageConfig != null && imageConfig.getMediaType() != null && imageConfig.getMediaType().equals(CONFIG_FILE_MEDIA_TYPE)) {
            return String.format("%s/%s", BLOBS_DIR_NAME, parsePathToBlobFileFromDigest(imageConfig.getDigest()));
        } else {
            throw new IntegrationException("Unable to find config file");
        }
    }
    
    private String getConfigDigestFromRootManifestText(String rootManifestFileText) {
        int start = rootManifestFileText.indexOf(CONFIG_FIELD_NAME) + CONFIG_FIELD_NAME.length();
        int end = rootManifestFileText.indexOf('"', start + 1);
        if (start > -1 && end > start) {
            return rootManifestFileText.substring(start, end).replace(":", "/");
        }
        return null;
    }
}
