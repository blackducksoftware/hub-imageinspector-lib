package com.blackducksoftware.integration.hub.imageinspector.name;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ImageNameResolverTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testSimple() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine:latest");
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithoutTag() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine");
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithUrlPortTag() {
        final ImageNameResolver resolver = new ImageNameResolver("https://artifactory.team.domain.com:5002/repo:tag");
        assertEquals("https://artifactory.team.domain.com:5002/repo", resolver.getNewImageRepo().get());
        assertEquals("tag", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithUrlPortNoTag() {
        final ImageNameResolver resolver = new ImageNameResolver("https://artifactory.team.domain.com:5002/repo");
        assertEquals("https://artifactory.team.domain.com:5002/repo", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithUrlTag() {
        final ImageNameResolver resolver = new ImageNameResolver("https://artifactory.team.domain.com/repo:tag");
        assertEquals("https://artifactory.team.domain.com/repo", resolver.getNewImageRepo().get());
        assertEquals("tag", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithUrlNoTag() {
        final ImageNameResolver resolver = new ImageNameResolver("https://artifactory.team.domain.com/repo");
        assertEquals("https://artifactory.team.domain.com/repo", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

}
