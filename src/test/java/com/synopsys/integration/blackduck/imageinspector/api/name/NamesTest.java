package com.synopsys.integration.blackduck.imageinspector.api.name;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NamesTest {

    @Test
    public void testCodeLocationName() {
        assertEquals("prefix_imageName_imageTag_pkgMgrName", Names.getCodeLocationName("prefix", "imageName", "imageTag", "pkgMgrName", false));
    }
}
