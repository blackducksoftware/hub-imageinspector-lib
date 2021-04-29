package com.synopsys.integration.blackduck.imageinspector.api.name;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ImageNameResolverTest {

    @BeforeAll
    public static void setUpBeforeAll() throws Exception {
    }

    @AfterAll
    public static void tearDownAfterAll() throws Exception {
    }

    @Test
    public void testSimple() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine:latest");
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testNull() {
        final ImageNameResolver resolver = new ImageNameResolver("null:null");
        assertEquals("null", resolver.getNewImageRepo().get());
        assertEquals("null", resolver.getNewImageTag().get());
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

    @Test
    public void testNone() {
        final ImageNameResolver resolver = new ImageNameResolver("");
        assertFalse(resolver.getNewImageRepo().isPresent());
        assertFalse(resolver.getNewImageTag().isPresent());
    }

    @Test
    public void testRepoOnly() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine");
        assertTrue(resolver.getNewImageRepo().isPresent());
        assertTrue(resolver.getNewImageTag().isPresent());
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testBoth() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine:1.0");
        assertTrue(resolver.getNewImageRepo().isPresent());
        assertTrue(resolver.getNewImageTag().isPresent());
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("1.0", resolver.getNewImageTag().get());
    }

    @Test
    public void testBothColonInRepoSpecifier() {
        final ImageNameResolver resolver = new ImageNameResolver("artifactoryserver:5000/alpine:1.0");
        assertTrue(resolver.getNewImageRepo().isPresent());
        assertTrue(resolver.getNewImageTag().isPresent());
        assertEquals("artifactoryserver:5000/alpine", resolver.getNewImageRepo().get());
        assertEquals("1.0", resolver.getNewImageTag().get());
    }

    @Test
    public void testEndsWithColon() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine:");
        assertTrue(resolver.getNewImageRepo().isPresent());
        assertTrue(resolver.getNewImageTag().isPresent());
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

}
