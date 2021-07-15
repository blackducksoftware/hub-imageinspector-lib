/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.synopsys.integration.bdio.model.SimpleBdioDocument;

// Comprehensive info about an image, including
// the harder-to-derive bits
public class ImageInfoDerived {
    private final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb;
    private ManifestLayerMapping manifestLayerMapping = null;
    private String codeLocationName = null;
    private String finalProjectName = null;
    private String finalProjectVersionName = null;
    private SimpleBdioDocument bdioDocument = null;

    public ImageInfoDerived(final ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb) {
        this.containerFileSystemWithPkgMgrDb = containerFileSystemWithPkgMgrDb;
    }

    public ManifestLayerMapping getManifestLayerMapping() {
        return manifestLayerMapping;
    }

    public void setManifestLayerMapping(final ManifestLayerMapping manifestLayerMapping) {
        this.manifestLayerMapping = manifestLayerMapping;
    }

    public ContainerFileSystemWithPkgMgrDb getImageInfoParsed() {
        return containerFileSystemWithPkgMgrDb;
    }

    public String getCodeLocationName() {
        return codeLocationName;
    }

    public void setCodeLocationName(final String codeLocationName) {
        this.codeLocationName = codeLocationName;
    }

    public String getFinalProjectName() {
        return finalProjectName;
    }

    public void setFinalProjectName(final String finalProjectName) {
        this.finalProjectName = finalProjectName;
    }

    public String getFinalProjectVersionName() {
        return finalProjectVersionName;
    }

    public void setFinalProjectVersionName(final String finalProjectVersionName) {
        this.finalProjectVersionName = finalProjectVersionName;
    }

    public SimpleBdioDocument getBdioDocument() {
        return bdioDocument;
    }

    public void setBdioDocument(final SimpleBdioDocument bdioDocument) {
        this.bdioDocument = bdioDocument;
    }
}
