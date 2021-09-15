package com.synopsys.integration.blackduck.imageinspector.api.name;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.NameValuePair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ImageNameResolverTest {
    private static ImageNameResolver resolver;

    @BeforeAll
    public static void setUpBeforeAll() throws Exception {
        resolver = new ImageNameResolver();
    }

    @AfterAll
    public static void tearDownAfterAll() throws Exception {
    }

    @Test
    public void testSimple() {
        NameValuePair resolvedRepoTag = resolver.resolve("alpine:latest", null, null);
        assertEquals("alpine", resolvedRepoTag.getName());
        assertEquals("latest", resolvedRepoTag.getValue());
    }

    @Test
    public void testNullLiterals() {
        NameValuePair resolvedRepoTag = resolver.resolve(null, "givenRepo", "givenTag");
        assertEquals("givenRepo", resolvedRepoTag.getName());
        assertEquals("givenTag", resolvedRepoTag.getValue());
    }

    @Test
    public void testWithoutTag() {
        NameValuePair resolvedRepoTag = resolver.resolve("alpine", null, null);
        assertEquals("alpine", resolvedRepoTag.getName());
        assertEquals("latest", resolvedRepoTag.getValue());
    }

    @Test
    public void testWithUrlPortTag() {
        NameValuePair resolvedRepoTag = resolver.resolve("https://artifactory.team.domain.com:5002/repo:tag", null, null);
        assertEquals("https://artifactory.team.domain.com:5002/repo", resolvedRepoTag.getName());
        assertEquals("tag", resolvedRepoTag.getValue());
    }

    @Test
    public void testWithUrlPortNoTag() {
        NameValuePair resolvedRepoTag = resolver.resolve("https://artifactory.team.domain.com:5002/repo", null, null);
        assertEquals("https://artifactory.team.domain.com:5002/repo", resolvedRepoTag.getName());
        assertEquals("latest", resolvedRepoTag.getValue());
    }

    @Test
    public void testWithUrlTag() {
        NameValuePair resolvedRepoTag = resolver.resolve("https://artifactory.team.domain.com/repo:tag", null, null);
        assertEquals("https://artifactory.team.domain.com/repo", resolvedRepoTag.getName());
        assertEquals("tag", resolvedRepoTag.getValue());
    }

    @Test
    public void testWithUrlNoTag() {
        NameValuePair resolvedRepoTag = resolver.resolve("https://artifactory.team.domain.com/repo", null, null);
        assertEquals("https://artifactory.team.domain.com/repo", resolvedRepoTag.getName());
        assertEquals("latest", resolvedRepoTag.getValue());
    }


    @Test
    public void testNull() {
        NameValuePair resolvedRepoTag = resolver.resolve(null, null, null);
        assertEquals("", resolvedRepoTag.getName());
        assertEquals("", resolvedRepoTag.getValue());
    }

    @Test
    public void testNone() {
        NameValuePair resolvedRepoTag = resolver.resolve("", null, null);
        assertEquals("", resolvedRepoTag.getName());
        assertEquals("", resolvedRepoTag.getValue());
    }

    @Test
    public void testRepoOnly() {
        NameValuePair resolvedRepoTag = resolver.resolve("alpine", null, null);
        assertEquals("alpine", resolvedRepoTag.getName());
        assertEquals("latest", resolvedRepoTag.getValue());
    }

    @Test
    public void testBoth() {
        NameValuePair resolvedRepoTag = resolver.resolve("alpine:1.0", null, null);
        assertEquals("alpine", resolvedRepoTag.getName());
        assertEquals("1.0", resolvedRepoTag.getValue());
    }

    @Test
    public void testBothColonInRepoSpecifier() {
        NameValuePair resolvedRepoTag = resolver.resolve("artifactoryserver:5000/alpine:1.0", null, null);
        assertEquals("artifactoryserver:5000/alpine", resolvedRepoTag.getName());
        assertEquals("1.0", resolvedRepoTag.getValue());
    }

    @Test
    public void testEndsWithColon() {
        NameValuePair resolvedRepoTag = resolver.resolve("alpine:", "", "");
        assertEquals("alpine", resolvedRepoTag.getName());
        assertEquals("latest", resolvedRepoTag.getValue());
    }

}
