package com.blackduck.integration.blackduck.imageinspector.image.common;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ManifestRepoTagMatcherTest {
    @ParameterizedTest
    @MethodSource("testFindMatchProvider")
    public void testFindMatch(String manifestRepoTag, String targetRepoTag, boolean matches) {
        ManifestRepoTagMatcher matcher = new ManifestRepoTagMatcher();
        Assertions.assertEquals(matches, matcher.findMatch(manifestRepoTag, targetRepoTag).isPresent());
    }

    private static Stream<Arguments> testFindMatchProvider() {
        return Stream.of(
            arguments("repo:tag", "org/repo:tag", true),
            arguments("repo:tag", "repo:tag", true),
            arguments("org/repo", "repo:tag", false),
            arguments("org/repo:tag", "repo:tag", false) //TODO- should this be expected?
        );
    }
}
