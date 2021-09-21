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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OciManifestDescriptorParser {
    private static final String MANIFEST_FILE_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ManifestRepoTagMatcher manifestRepoTagMatcher;

    public OciManifestDescriptorParser(ManifestRepoTagMatcher manifestRepoTagMatcher) {
        this.manifestRepoTagMatcher = manifestRepoTagMatcher;
    }

    public OciDescriptor getManifestDescriptor(OciImageIndex ociImageIndex,
                                               @Nullable String givenRepo, @Nullable String givenTag) throws IntegrationException {
        // TODO- Probably also need to select one of multiple based on arch
        List<OciDescriptor> trueManifests =
                ociImageIndex.getManifests().stream()
                        .filter(man -> MANIFEST_FILE_MEDIA_TYPE.equals(man.getMediaType()))
                        .collect(Collectors.toList());
        if (trueManifests.size() == 0) {
            throw new IntegrationException(String.format("No manifest descriptor with media type %s was found in OCI image index", MANIFEST_FILE_MEDIA_TYPE));
        }
        if ((trueManifests.size() == 1) && StringUtils.isBlank(givenRepo)) {
            logger.debug(String.format("User did not specify a repo:tag, and there's only one manifest; inspecting that one; digest=%s", trueManifests.get(0).getDigest()));
            return trueManifests.get(0);
        }
        if ((trueManifests.size() > 1) && StringUtils.isBlank(givenRepo)) {
            throw new IntegrationException("When the image contains multiple manifests, the target image and tag to inspect must be specified");
        }
        if (StringUtils.isNotBlank(givenRepo) && StringUtils.isBlank(givenTag)) {
            givenTag = "latest";
        }
        // Safe to assume both repo and tag have values
        String givenRepoTag = String.format("%s:%s", givenRepo, givenTag);

        Optional<OciDescriptor> matchingManifest = trueManifests.stream()
                                                       .filter(m -> m.getRepoTagString().isPresent())
                                                       .filter(m -> manifestRepoTagMatcher.findMatch(m.getRepoTagString().get(), givenRepoTag).isPresent())
                                                       .findFirst();
        if (!matchingManifest.isPresent()) {
            throw new IntegrationException(String.format("Unable to find manifest matching repo:tag: %s", givenRepoTag));
        }
        return matchingManifest.get();
    }
}
