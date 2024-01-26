package com.synopsys.integration.blackduck.imageinspector.containerfilesystem;

public class DataStripper {
    
    public static String stripEpocFromVersion(String version) {
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