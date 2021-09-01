package com.synopsys.integration.blackduck.imageinspector.api.name;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NamesTest {

    @Test
    public void testCodeLocationNameRepoTag() {
        assertEquals("prefix_imageName_imageTag_pkgMgrName", Names.getCodeLocationName("prefix", "imageName", "imageTag", null, "pkgMgrName", false));
    }

    @Test
    public void testCodeLocationNameArchiveNameNormal() {
        assertEquals("prefix_alpine_tar_pkgMgrName", Names.getCodeLocationName("prefix", null, "", "alpine.tar", "pkgMgrName", false));
    }

    @Test
    public void testCodeLocationNameArchiveNameNoExt() {
        assertEquals("prefix_alpine_unknown_pkgMgrName", Names.getCodeLocationName("prefix", null, "", "alpine", "pkgMgrName", false));
    }

    @Test
    public void testCodeLocationNameArchiveNameNoExtEndsInDot() {
        assertEquals("prefix_alpine_unknown_pkgMgrName", Names.getCodeLocationName("prefix", null, "", "alpine.", "pkgMgrName", false));
    }

    @Test
    public void testCodeLocationNameArchiveNameNoExtStartsWithDot() {
        assertEquals("prefix_alpine_unknown_pkgMgrName", Names.getCodeLocationName("prefix", null, "", ".alpine", "pkgMgrName", false));
    }

    @Test
    void testProjectNameFromRepo() {
        assertEquals("alpine", Names.getBlackDuckProjectNameFromImageName("alpine", null, false));
    }

    @Test
    void testProjectNameFromFilename() {
        assertEquals("ubuntu", Names.getBlackDuckProjectNameFromImageName(null, "ubuntu.tar", false));
    }

    @Test
    void testProjectVersionNameFromTag() {
        assertEquals("latest", Names.getBlackDuckProjectVersionNameFromImageTag("latest"));
    }

    @Test
    void testProjectVersionNameFromBlankTag() {
        assertEquals("unknown", Names.getBlackDuckProjectVersionNameFromImageTag(""));
    }
}
