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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
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

    private static final String INDEX_FILE_MEDIA_TYPE = "application/vnd.oci.image.index.v1+json";
    private static final String CONFIG_FILE_MEDIA_TYPE = "application/vnd.oci.image.config.v1+json";

    private static final String LAYER_ARCHIVE_TAR_MEDIA_TYPE_SUFFIX = ".tar";
    private static final String LAYER_ARCHIVE_TAR_GZIP_MEDIA_TYPE_SUFFIX = ".tar+gzip";
    private static final String LAYER_ARCHIVE_TAR_ZSTD_MEDIA_TYPE_SUFFIX = ".tar+zstd";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Gson gson;
    private FileOperations fileOperations;
    private final CommonImageConfigParser commonImageConfigParser;
    private final OciImageIndexFileParser ociImageIndexFileParser;

    public OciImageDirectoryExtractor(final Gson gson, FileOperations fileOperations, CommonImageConfigParser commonImageConfigParser,
                                      OciImageIndexFileParser ociImageIndexFileParser) {
        this.gson = gson;
        this.fileOperations = fileOperations;
        this.commonImageConfigParser = commonImageConfigParser;
        this.ociImageIndexFileParser = ociImageIndexFileParser;
    }

    @Override
    public List<TypedArchiveFile> getLayerArchives(final File imageDir) throws IntegrationException {
        File blobsDir = new File(imageDir, BLOBS_DIR_NAME);

        Optional<File> manifestFile = findManifestFile(imageDir);
        if (!manifestFile.isPresent()) {
            logger.trace("Could not find manifest file.");
            return new LinkedList<>();
        }

        try { //TODO- this is probably not the best way to handle this exception...
            return parseLayerArchives(manifestFile.get(), blobsDir);
        } catch (IOException e) {
            throw new IntegrationException(e.getMessage());
        }
    }

    private Optional<File> findManifestFile(File imageDir) throws IntegrationException {
        File indexFile = new File (imageDir, INDEX_FILE_NAME);

        OciImageIndex ociImageIndex = ociImageIndexFileParser.loadIndex(indexFile);
        String manifestFileDigest = ociImageIndexFileParser.parseManifestFileDigestFromImageIndex(ociImageIndex);
        File blobsDir = new File(imageDir, BLOBS_DIR_NAME);

        String pathToManifestFile = parsePathToBlobFileFromDigest(manifestFileDigest);
        File manifestFile;
        try {
            manifestFile = findBlob(blobsDir, pathToManifestFile);
        } catch (IntegrationException e) {
            logger.error(e.getMessage());
            return Optional.empty();
        }
        return Optional.of(manifestFile);
    }

    private ArchiveFileType parseArchiveTypeFromLayerDescriptorMediaType(String mediaType) throws IntegrationException {
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
            throw new IntegrationException(String.format("Unrecognized layer media type: %s", mediaType));
        }
    }

    private List<TypedArchiveFile> parseLayerArchives(File manifestFile, File blobsDir) throws IOException {
        // Parse manifest file for names + archive formats of layer files
        String manifestFileText = fileOperations.readFileToString(manifestFile);
        OciImageManifest imageManifest = gson.fromJson(manifestFileText, OciImageManifest.class);

        List<TypedArchiveFile> layerArchives = new LinkedList<>();
        for (OciDescriptor layer : imageManifest.getLayers()) {
            String pathToLayerFile = parsePathToBlobFileFromDigest(layer.getDigest());
            File layerFile;
            try {
                layerFile = findBlob(blobsDir, pathToLayerFile);
            } catch (IntegrationException e) {
                logger.error(e.getMessage());
                continue;
            }

            ArchiveFileType archiveFileType;
            try {
                archiveFileType = parseArchiveTypeFromLayerDescriptorMediaType(layer.getMediaType());
            } catch (IntegrationException e) {
                logger.trace(e.getMessage());
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

    private File findBlob(File blobsDir, String pathToBlob) throws IntegrationException {
        File blob = new File(blobsDir, pathToBlob);
        if (!blob.exists()) {
            throw new IntegrationException(String.format("Blob referenced by image manifest could not be found at %s.", blob.getAbsolutePath()));
        }
        return blob;
    }

    @Override
    public FullLayerMapping getLayerMapping(final File imageDir, @Nullable String repo, @Nullable String tag) throws IntegrationException {

        OciImageManifest imageManifest;
        // TODO use exception instead of optional
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

        imageManifest = gson.fromJson(manifestFileText, OciImageManifest.class);


        String pathToImageConfigFileFromRoot = findImageConfigFilePath(imageManifest);
        List<String> layerInternalIds = imageManifest.getLayers().stream()
                                            .map(OciDescriptor::getDigest)
                                            .collect(Collectors.toList());

        ManifestLayerMapping manifestLayerMapping = new ManifestLayerMapping(repo, tag, pathToImageConfigFileFromRoot, layerInternalIds);

        List<String> layerExternalIds = commonImageConfigParser.getExternalLayerIdsFromImageConfigFile(imageDir, pathToImageConfigFileFromRoot);
        return new FullLayerMapping(manifestLayerMapping, layerExternalIds);
    }

    private String ensurePopulated(String field, String defaultValue) {
        if (StringUtils.isBlank(field)) {
            field = defaultValue;
        }
        return field;
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
