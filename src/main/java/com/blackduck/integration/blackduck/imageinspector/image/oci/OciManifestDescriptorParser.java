/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2025 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.image.oci;

import com.blackduck.integration.blackduck.imageinspector.image.common.ManifestRepoTagMatcher;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciDescriptor;
import com.blackduck.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.blackduck.integration.exception.IntegrationException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OciManifestDescriptorParser {
    private static final String MANIFEST_FILE_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";
    private static final String INDEX_FILE_MEDIA_TYPE = "application/vnd.oci.image.index.v1+json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ManifestRepoTagMatcher manifestRepoTagMatcher;

    public OciManifestDescriptorParser(ManifestRepoTagMatcher manifestRepoTagMatcher) {
        this.manifestRepoTagMatcher = manifestRepoTagMatcher;
    }

    public OciDescriptor getManifestDescriptor(OciImageIndex ociImageIndex,
        @Nullable String givenRepo, @Nullable String givenTag) throws IntegrationException {
        // TODO- Probably also need to select one of multiple based on arch
        List<OciDescriptor> trueManifests = new ArrayList<>();
        for (OciDescriptor ociDescriptor : ociImageIndex.getManifests()) {
            logger.debug("Found a media type in manifest: {}", ociDescriptor.getMediaType());
            if (MANIFEST_FILE_MEDIA_TYPE.equals(ociDescriptor.getMediaType()) || INDEX_FILE_MEDIA_TYPE.equals(ociDescriptor.getMediaType())) {
                trueManifests.add(ociDescriptor);
            }
        }
        if (trueManifests.isEmpty()) {
            throw new IntegrationException(String.format("No manifest descriptor with either media type {} or {} was found in OCI image index", INDEX_FILE_MEDIA_TYPE, MANIFEST_FILE_MEDIA_TYPE));
        }
        if ((trueManifests.size() == 1)) {
            logger.debug(String.format("There is only one manifest; inspecting that one; digest={}", trueManifests.get(0).getDigest()));
            return trueManifests.get(0);
        }
        if ((trueManifests.size() > 1) && StringUtils.isBlank(givenRepo)) {
            throw new IntegrationException("When the image contains multiple manifests, the target image and tag to inspect must be specified");
        }
        if (StringUtils.isNotBlank(givenRepo) && StringUtils.isBlank(givenTag)) {
            logger.debug("Tag value was not provided; resolving the tag value as \"latest\"");
            givenTag = "latest";
        }

        // Safe to assume both repo and tag have values at this point
        String givenRepoTag = String.format("%s:%s", givenRepo, givenTag);

        Optional<OciDescriptor> matchingManifest = trueManifests.stream()
            .filter(m -> m.getRepoTagString().isPresent())
            .filter(m -> manifestRepoTagMatcher.findMatch(m.getRepoTagString().get(), givenRepoTag).isPresent())
            .findFirst();
        if (!matchingManifest.isPresent()) {
            throw new IntegrationException(String.format("Unable to find manifest matching repo:tag: {}", givenRepoTag));
        }
        return matchingManifest.get();
    }
}
