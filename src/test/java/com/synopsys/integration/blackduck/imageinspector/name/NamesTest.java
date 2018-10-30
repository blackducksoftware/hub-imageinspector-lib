package com.synopsys.integration.blackduck.imageinspector.name;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NamesTest {

    @Test
    public void testContainerFileSystemTarFilename() {
        assertEquals("alpine_3.6_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename("alpine:3.6", ""));
        assertEquals("alpine_3.6_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename(null, "alpine_3.6.tar"));
        assertEquals("blackducksoftware_hub-webapp_4.2.0_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename("blackducksoftware/hub-webapp:4.2.0", null));
        assertEquals("1.2.3_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename("", "1.2.3.tar"));
        assertEquals("123_containerfilesystem.tar.gz", Names.getContainerFileSystemTarFilename("", "123"));
    }

    @Test
    public void testCodeLocationName() {
        assertEquals("prefix_imageName_imageTag_pkgMgrName", Names.getCodeLocationName("prefix", "imageName", "imageTag", "pkgMgrName"));
    }

    @Test
    public void testImageTarfilename() {
        assertEquals("a_b_c_latest.tar", Names.getImageTarFilename("a/b/c", "latest"));
        assertEquals("a_b_c_latest.tar", Names.getImageTarFilename("a/b:c", "latest"));
        assertEquals("solsson_kafka-prometheus-jmx-exporter__sha256_a23062396cd5af1acdf76512632c20ea6be76885dfc20cd9ff40fb23846557e8.tar", Names.getImageTarFilename("solsson/kafka-prometheus-jmx-exporter", "@sha256:a23062396cd5af1acdf76512632c20ea6be76885dfc20cd9ff40fb23846557e8"));
        assertEquals("solsson_kafka-prometheus-jmx-exporter_sha256_a23062396cd5af1acdf76512632c20ea6be76885dfc20cd9ff40fb23846557e8_.tar", Names.getImageTarFilename("solsson/kafka-prometheus-jmx-exporter@sha256:a23062396cd5af1acdf76512632c20ea6be76885dfc20cd9ff40fb23846557e8", null));
    }
}
