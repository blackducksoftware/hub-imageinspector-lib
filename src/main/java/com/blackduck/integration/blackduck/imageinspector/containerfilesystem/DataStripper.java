/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem;

public class DataStripper {
    
    public static String stripEpochFromVersion(String version) {
        return version != null && version.startsWith("0:") ? version.substring(2) : version;
    }
    
    public static String stripEpochFromExternalId(String externalId) {
        int pos;
        if (externalId != null && (pos = externalId.indexOf("/0:")) > -1) {
            return externalId.substring(0, pos + 1).concat(externalId.substring(pos + 3));
        }
        return externalId;
    }
    
}