/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.image.oci;

import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciDescriptor;
import com.synopsys.integration.blackduck.imageinspector.image.oci.model.OciImageIndex;
import com.synopsys.integration.exception.IntegrationException;

public class OciManifestDescriptorParser {
    private static final String MANIFEST_FILE_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";

    public OciDescriptor getManifestDescriptor(OciImageIndex ociImageIndex) throws IntegrationException {
        // TODO right now this just returns the first one; probably not adequate
        // Probably need to be able to select one of many (based on repo:tag? and/or arch maybe?)
        for (OciDescriptor manifestData : ociImageIndex.getManifests()) {
            if (manifestData.getMediaType().equals(MANIFEST_FILE_MEDIA_TYPE)) {
                    return manifestData;
            }
        }
        throw new IntegrationException(String.format("Manifest descriptor with media type %s not found in OCI image index", MANIFEST_FILE_MEDIA_TYPE));
    }

    public String getManifestFileDigest(OciImageIndex ociImageIndex) throws IntegrationException {
        return getManifestDescriptor(ociImageIndex).getDigest();
    }
}
