package com.synopsys.integration.blackduck.imageinspector.api.name;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synopsys.integration.blackduck.imageinspector.image.common.RepoTag;
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
        RepoTag resolvedRepoTag = resolver.resolve("alpine:latest", null, null);
        assertEquals("alpine", resolvedRepoTag.getRepo().get());
        assertEquals("latest", resolvedRepoTag.getTag().get());
    }

    @Test
    public void testNullLiterals() {
        RepoTag resolvedRepoTag = resolver.resolve(null, "givenRepo", "givenTag");
        assertEquals("givenRepo", resolvedRepoTag.getRepo().get());
        assertEquals("givenTag", resolvedRepoTag.getTag().get());
    }

    @Test
    public void testWithoutTag() {
        RepoTag resolvedRepoTag = resolver.resolve("alpine", null, null);
        assertEquals("alpine", resolvedRepoTag.getRepo().get());
        assertEquals("latest", resolvedRepoTag.getTag().get());
    }

    @Test
    public void testWithUrlPortTag() {
        RepoTag resolvedRepoTag = resolver.resolve("https://artifactory.team.domain.com:5002/repo:tag", null, null);
        assertEquals("https://artifactory.team.domain.com:5002/repo", resolvedRepoTag.getRepo().get());
        assertEquals("tag", resolvedRepoTag.getTag().get());
    }

    @Test
    public void testWithUrlPortNoTag() {
        RepoTag resolvedRepoTag = resolver.resolve("https://artifactory.team.domain.com:5002/repo", null, null);
        assertEquals("https://artifactory.team.domain.com:5002/repo", resolvedRepoTag.getRepo().get());
        assertEquals("latest", resolvedRepoTag.getTag().get());
    }

    @Test
    public void testWithUrlTag() {
        RepoTag resolvedRepoTag = resolver.resolve("https://artifactory.team.domain.com/repo:tag", null, null);
        assertEquals("https://artifactory.team.domain.com/repo", resolvedRepoTag.getRepo().get());
        assertEquals("tag", resolvedRepoTag.getTag().get());
    }

    @Test
    public void testWithUrlNoTag() {
        RepoTag resolvedRepoTag = resolver.resolve("https://artifactory.team.domain.com/repo", null, null);
        assertEquals("https://artifactory.team.domain.com/repo", resolvedRepoTag.getRepo().get());
        assertEquals("latest", resolvedRepoTag.getTag().get());
    }


    @Test
    public void testNull() {
        RepoTag resolvedRepoTag = resolver.resolve(null, null, null);
        assertFalse(resolvedRepoTag.getRepo().isPresent());
        assertFalse(resolvedRepoTag.getTag().isPresent());
    }

    @Test
    public void testNone() {
        RepoTag resolvedRepoTag = resolver.resolve("", null, null);
        assertFalse(resolvedRepoTag.getRepo().isPresent());
        assertFalse(resolvedRepoTag.getTag().isPresent());
    }

    @Test
    public void testBoth() {
        RepoTag resolvedRepoTag = resolver.resolve("alpine:1.0", null, null);
        assertEquals("alpine", resolvedRepoTag.getRepo().get());
        assertEquals("1.0", resolvedRepoTag.getTag().get());
    }

    @Test
    public void testBothColonInRepoSpecifier() {
        RepoTag resolvedRepoTag = resolver.resolve("artifactoryserver:5000/alpine:1.0", null, null);
        assertEquals("artifactoryserver:5000/alpine", resolvedRepoTag.getRepo().get());
        assertEquals("1.0", resolvedRepoTag.getTag().get());
    }

    @Test
    public void testEndsWithColon() {
        RepoTag resolvedRepoTag = resolver.resolve("alpine:", "", "");
        assertEquals("alpine", resolvedRepoTag.getRepo().get());
        assertEquals("latest", resolvedRepoTag.getTag().get());
    }

}
