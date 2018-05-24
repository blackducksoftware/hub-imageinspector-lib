package com.blackducksoftware.integration.hub.imageinspector.name;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NamesTest {

    @Test
    public void test() {
        assertEquals("alpine_3.6_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename("alpine:3.6", ""));
        assertEquals("alpine_3.6_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename(null, "alpine_3.6.tar"));
        assertEquals("blackducksoftware_hub-webapp_4.2.0_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename("blackducksoftware/hub-webapp:4.2.0", null));
        assertEquals("1.2.3_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename("", "1.2.3.tar"));
        assertEquals("123_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename("", "123"));
    }
}
