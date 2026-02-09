package com.blackduck.integration.blackduck.imageinspector.image.oci.util;

import com.blackduck.integration.blackduck.imageinspector.image.oci.OciImageIndexFileParser;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciDescriptor;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.blackduck.integration.exception.IntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class OciImageHelper {

    private static final String INDEX_FILE_NAME = "index.json";
    private OciImageIndexFileParser  ociImageIndexFileParser;
    private static final String BLOBS_DIR_NAME = "blobs";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public OciImageHelper(OciImageIndexFileParser ociImageIndexFileParser) {
        this.ociImageIndexFileParser = ociImageIndexFileParser;
    }

    public OciImageIndex extractOciImageIndex(File imageDir) throws IntegrationException {
        File indexFile = new File (imageDir, INDEX_FILE_NAME);
        return ociImageIndexFileParser.loadIndex(indexFile);
    }

    public File findManifestFile(File imageDir, OciDescriptor manifestDescriptor) throws IntegrationException {
        String manifestFileDigest = manifestDescriptor.getDigest();

        String pathToManifestFile = parsePathToBlobFileFromDigest(manifestFileDigest);
        logger.trace("Path to manifest file: {}", pathToManifestFile);
        File blobsDir = new File(imageDir, BLOBS_DIR_NAME);
        return findBlob(blobsDir, pathToManifestFile);
    }

    // Digests are in the format <hash algorithm>:<hash of content> and the path to the file from "blobs" dir is <hash algorithm>/<hash of contents
    public String parsePathToBlobFileFromDigest(String digest) {
        return String.join("/", digest.split(":"));
    }

    public File findBlob(File blobsDir, String pathToBlob) throws IntegrationException {
        File blob = new File(blobsDir, pathToBlob);
        if (!blob.exists()) {
            throw new IntegrationException(String.format("Blob referenced by image manifest could not be found at %s.", blob.getAbsolutePath()));
        }
        return blob;
    }

    public OciImageIndex loadIndex(File indexFile) throws IntegrationException {
        return ociImageIndexFileParser.loadIndex(indexFile);
    }

}
