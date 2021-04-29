package com.synopsys.integration.blackduck.imageinspector.api.name;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NamesTest {

    @Test
    public void testCodeLocationName() {
        assertEquals("prefix_imageName_imageTag_pkgMgrName", Names.getCodeLocationName("prefix", "imageName", "imageTag", "pkgMgrName", false));
    }
}
