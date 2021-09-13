/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import com.synopsys.integration.blackduck.imageinspector.image.common.ManifestRepoTagMatcher;
import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciDescriptor;
import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OciManifestDescriptorParser {
    private static final String MANIFEST_FILE_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";
    private final ManifestRepoTagMatcher manifestRepoTagMatcher;

    public OciManifestDescriptorParser(ManifestRepoTagMatcher manifestRepoTagMatcher) {
        this.manifestRepoTagMatcher = manifestRepoTagMatcher;
    }

    public OciDescriptor getManifestDescriptor(OciImageIndex ociImageIndex,
                                               @Nullable String givenRepo, @Nullable String givenTag) throws IntegrationException {
        // Probably also need to select one of multiple based on arch
        List<OciDescriptor> trueManifests =
                ociImageIndex.getManifests().stream()
                        .filter(man -> MANIFEST_FILE_MEDIA_TYPE.equals(man.getMediaType()))
                        .collect(Collectors.toList());
        if (trueManifests.size() == 0) {
            throw new IntegrationException(String.format("No manifest descriptor with media type %s was found in OCI image index", MANIFEST_FILE_MEDIA_TYPE));
        }
        if ((trueManifests.size() == 1) && StringUtils.isBlank(givenRepo)) {
            return trueManifests.get(0);
        }
        if (StringUtils.isBlank(givenTag)) {
            givenTag = "latest";
        }
        // Both repo and tag have values
        String givenRepoTag = String.format("%s:%s", givenRepo, givenTag);

        // TODO is there a simpler way to do this?
        List <String> manifestRepTagStrings = trueManifests.stream()
                .map(OciDescriptor::getRepoTagString)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        Optional<String> matchingManifestRepoTagString = manifestRepoTagMatcher.findMatch(manifestRepTagStrings, givenRepoTag);
        if (!matchingManifestRepoTagString.isPresent()) {
            throw new IntegrationException(String.format("No manifest found matching given repo:tag: %s", givenRepoTag));
        }
        // We know which repo:tag we want; return manifest matching that
        Optional<OciDescriptor> matchingManifest = trueManifests.stream()
                .filter(m -> m.getRepoTagString().isPresent())
                .filter(m -> m.getRepoTagString().get().equals(matchingManifestRepoTagString.get()))
                .findFirst();
        if (!matchingManifest.isPresent()) {
            throw new IntegrationException(String.format("Unable to find manifest matching repo:tag: %s", matchingManifestRepoTagString.get()));
        }
        return matchingManifest.get();
    }

    public String getManifestFileDigest(OciImageIndex ociImageIndex,
                                        @Nullable String givenRepo, @Nullable String givenTag) throws IntegrationException {
        return getManifestDescriptor(ociImageIndex, givenRepo, givenTag).getDigest();
    }
}
