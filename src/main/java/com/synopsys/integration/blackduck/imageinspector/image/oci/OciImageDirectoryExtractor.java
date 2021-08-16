package com.synopsys.integration.blackduck.imageinspector.image.oci;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageDirectoryExtractor;
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
    private static final String LAYER_ARCHIVE_TAR_MEDIA_TYPE_SUFFIX = ".tar";
    private static final String LAYER_ARCHIVE_TAR_GZIP_MEDIA_TYPE_SUFFIX = ".tar+gzip";
    private static final String LAYER_ARCHIVE_TAR_ZSTD_MEDIA_TYPE_SUFFIX = ".tar+zstd";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Gson gson;
    private FileOperations fileOperations;

    public OciImageDirectoryExtractor(final GsonBuilder gsonBuilder, FileOperations fileOperations) {
        this.gson = gsonBuilder.create();
        this.fileOperations = fileOperations;
    }

    @Override
    public List<TypedArchiveFile> getLayerArchives(final File imageDir) throws IOException {
        File indexFile = new File (imageDir, INDEX_FILE_NAME);

        Optional<String> manifestFileDigest = finidManifestFileDigest(indexFile);
        if (!manifestFileDigest.isPresent()) {
            return new LinkedList<>();
        }

        File blobsDir = new File(imageDir, BLOBS_DIR_NAME);

        String pathToManifestFile = parsePathToBlobFileFromDigest(manifestFileDigest.get());
        File manifestFile = findBlob(blobsDir, pathToManifestFile);

        return parseLayerArchives(manifestFile, blobsDir);
    }

    private Optional<String> finidManifestFileDigest(File indexFile) throws IOException {
        // Parse index.json to find manifest file digest (we'll later use it to find the file from the root directory)
        String indexFileText = fileOperations.readFileToString(indexFile);
        OciImageIndex imageIndex = gson.fromJson(indexFileText, OciImageIndex.class);
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
        return Optional.ofNullable(manifestFileDigest);
    }

    private ArchiveFileType parseArchiveTypeFromLayerDescriptorMediaType(String mediaType) throws Exception {
        if (mediaType.contains("nondistributable")) {
            //TODO- what do we do with archives with layer restrictions?
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
        OciImageManifest imageManifest = gson.fromJson(manifestFileText, OciImageManifest.class);

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
        return null;
    }
}
