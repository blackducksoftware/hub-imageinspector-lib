/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb;

import java.util.List;
import java.util.Map;

public class DbRelationshipInfo {
    private Map<String, List<String>> compNamesToDependencies;
    private Map<String, String> providedBinariesToCompNames;

    public DbRelationshipInfo(final Map<String, List<String>> compNamesToDependencies, final Map<String, String> providedBinariesToCompNames) {
        this.compNamesToDependencies = compNamesToDependencies;
        this.providedBinariesToCompNames = providedBinariesToCompNames;
    }

    public Map<String, List<String>> getCompNamesToDependencies() {
        return compNamesToDependencies;
    }

    public Map<String, String> getProvidedBinariesToCompNames() {
        return providedBinariesToCompNames;
    }
}
