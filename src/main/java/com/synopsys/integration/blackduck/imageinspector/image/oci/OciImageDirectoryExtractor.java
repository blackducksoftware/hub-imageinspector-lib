/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryExtractor;
import com.synopsys.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciDescriptor;
import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciImageManifest;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;

public class OciImageDirectoryExtractor implements ImageDirectoryExtractor {
    private static final String INDEX_FILE_NAME = "index.json";
    private static final String BLOBS_DIR_NAME = "blobs";

    private static final String MANIFEST_FILE_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";
    private static final String INDEX_FILE_MEDIA_TYPE = "application/vnd.oci.image.index.v1+json";
    private static final String CONFIG_FILE_MEDIA_TYPE = "application/vnd.oci.image.config.v1+json";

    private static final String LAYER_ARCHIVE_TAR_MEDIA_TYPE_SUFFIX = ".tar";
    private static final String LAYER_ARCHIVE_TAR_GZIP_MEDIA_TYPE_SUFFIX = ".tar+gzip";
    private static final String LAYER_ARCHIVE_TAR_ZSTD_MEDIA_TYPE_SUFFIX = ".tar+zstd";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private GsonBuilder gsonBuilder;
    private FileOperations fileOperations;
    private final CommonImageConfigParser commonImageConfigParser;

    public OciImageDirectoryExtractor(final GsonBuilder gsonBuilder, FileOperations fileOperations, CommonImageConfigParser commonImageConfigParser) {
        this.gsonBuilder = gsonBuilder;
        this.fileOperations = fileOperations;
        this.commonImageConfigParser = commonImageConfigParser;
    }

    @Override
    public List<TypedArchiveFile> getLayerArchives(final File imageDir) throws IOException {
        File blobsDir = new File(imageDir, BLOBS_DIR_NAME);

        Optional<File> manifestFile = findManifestFile(imageDir);
        if (!manifestFile.isPresent()) {
            logger.trace("Could not find manifest file.");
            return new LinkedList<>();
        }

        return parseLayerArchives(manifestFile.get(), blobsDir);
    }

    private Optional<File> findManifestFile(File imageDir) {
        File indexFile = new File (imageDir, INDEX_FILE_NAME);

        Optional<String> manifestFileDigest = parseManifestFileDigestFromImageIndex(indexFile);
        if (!manifestFileDigest.isPresent()) {
            return Optional.empty();
        }

        File blobsDir = new File(imageDir, BLOBS_DIR_NAME);

        String pathToManifestFile = parsePathToBlobFileFromDigest(manifestFileDigest.get());
        return Optional.of(findBlob(blobsDir, pathToManifestFile));
    }

    private Optional<String> parseManifestFileDigestFromImageIndex(File indexFile) {
        // Parse index.json to find manifest file digest (we'll later use it to find the file from the root directory)
        String indexFileText;
        try {
            indexFileText = fileOperations.readFileToString(indexFile);
        } catch (IOException e) {
            return Optional.empty();
        }

        OciImageIndex imageIndex = gsonBuilder.create().fromJson(indexFileText, OciImageIndex.class);
        String manifestFileDigest = null;
        for (OciDescriptor manifestData : imageIndex.getManifests()) {
            if (manifestData.getMediaType().equals(MANIFEST_FILE_MEDIA_TYPE)) {
                if (manifestFileDigest == null) {
                    manifestFileDigest = manifestData.getDigest();
                } else {
                    //TODO- what to do if we find multiple manifests?  OCI specs mention sometimes there's one for each supported architecture
                }
            }
            if (manifestData.getMediaType().equals(INDEX_FILE_MEDIA_TYPE)) {
                //TODO- what to do if we find multiple image indexes?
            }
        }
        // Per specs, the size of OciImageIndex.manifests may be 0
        return Optional.ofNullable(manifestFileDigest);
    }

    private ArchiveFileType parseArchiveTypeFromLayerDescriptorMediaType(String mediaType) throws Exception {
        if (mediaType.contains("nondistributable")) {
            //TODO- what do we do with archives with distribution restrictions?
        }
        if (mediaType.endsWith(LAYER_ARCHIVE_TAR_MEDIA_TYPE_SUFFIX)) {
            return ArchiveFileType.TAR;
        } else if (mediaType.endsWith(LAYER_ARCHIVE_TAR_GZIP_MEDIA_TYPE_SUFFIX)) {
            return ArchiveFileType.TAR_GZIPPED;
        } else if (mediaType.endsWith(LAYER_ARCHIVE_TAR_ZSTD_MEDIA_TYPE_SUFFIX)) {
            return ArchiveFileType.TAR_ZSTD;
        } else {
            throw new Exception();
        }
    }

    private List<TypedArchiveFile> parseLayerArchives(File manifestFile, File blobsDir) throws IOException {
        // Parse manifest file for names + archive formats of layer files
        String manifestFileText = fileOperations.readFileToString(manifestFile);
        OciImageManifest imageManifest = gsonBuilder.create().fromJson(manifestFileText, OciImageManifest.class);

        List<TypedArchiveFile> layerArchives = new LinkedList<>();
        for (OciDescriptor layerData : imageManifest.getLayers()) {
            String pathToLayerFile = parsePathToBlobFileFromDigest(layerData.getDigest());
            File layerFile = findBlob(blobsDir, pathToLayerFile);
            ArchiveFileType archiveFileType;
            try {
                archiveFileType = parseArchiveTypeFromLayerDescriptorMediaType(layerData.getMediaType());
            } catch (Exception e) {
                logger.trace("Unrecognized layer media type: " + layerData.getMediaType());
                continue;
            }
            layerArchives.add(new TypedArchiveFile(archiveFileType, layerFile));
        }

        return layerArchives;
    }

    // Digests are in the format <hash algorithm>:<hash of content> and the path to the file from "blobs" dir is <hash algorithm>/<hash of contents>
    private String parsePathToBlobFileFromDigest(String digest) {
        return String.join("/", digest.split(":"));
    }

    private File findBlob(File blobsDir, String pathToBlob) {
        File blob = new File(blobsDir, pathToBlob);
        if (!blob.exists()) {
            //TODO- specs say "The blobs directory MAY be missing referenced blobs, in which case the missing blobs SHOULD be fulfilled by an external blob store" --> should we handle this case?
        }
        return blob;
    }

    @Override
    public FullLayerMapping getLayerMapping(final File imageDir, final String repo, final String tag) throws IntegrationException {

        OciImageManifest imageManifest;
        Optional<File> manifestFile = findManifestFile(imageDir);
        if (!manifestFile.isPresent()) {
            throw new IntegrationException("Could not find manifest file");
        }

        String manifestFileText;
        try {
            manifestFileText = fileOperations.readFileToString(manifestFile.get());
        } catch (IOException e) {
            throw new IntegrationException(String.format("Unable to parse manifest file %s", manifestFile.get().getAbsolutePath()));
        }

        imageManifest = gsonBuilder.create().fromJson(manifestFileText, OciImageManifest.class);


        String pathToImageConfigFileFromRoot = findImageConfigFilePath(imageManifest);
        List<String> layerInternalIds = imageManifest.getLayers().stream()
                                            .map(OciDescriptor::getDigest)
                                            .collect(Collectors.toList());
        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(repo, tag, pathToImageConfigFileFromRoot, layerInternalIds);

        List<String> layerExternalIds = commonImageConfigParser.getExternalLayerIdsFromImageConfigFile(imageDir, pathToImageConfigFileFromRoot);
        return new FullLayerMapping(manifestLayerMapping, layerExternalIds);
    }

    private String findImageConfigFilePath(OciImageManifest imageManifest) throws IntegrationException {
        OciDescriptor imageConfig = imageManifest.getConfig();
        if (imageConfig.getMediaType().equals(CONFIG_FILE_MEDIA_TYPE)) {
            return String.format("%s/%s", BLOBS_DIR_NAME, parsePathToBlobFileFromDigest(imageConfig.getDigest()));
        } else {
            throw new IntegrationException("Unable to find config file");
        }
    }

}
