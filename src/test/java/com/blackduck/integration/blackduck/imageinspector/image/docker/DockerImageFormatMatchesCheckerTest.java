package com.blackduck.integration.blackduck.imageinspector.image.docker;

import com.blackduck.integration.blackduck.imageinspector.image.common.ImageFormatMatchesChecker;
import com.blackduck.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class DockerImageFormatMatchesCheckerTest {

    @Test
    void testTrue() throws IntegrationException {
        File imageDir = new File("src/test/resources/mockDockerTarContents");
        ImageFormatMatchesChecker dockerImageFormatMatchesChecker = new DockerImageFormatMatchesChecker();

        assertTrue(dockerImageFormatMatchesChecker.applies(imageDir));
    }

    @Test
    void testFalse() throws IntegrationException {
        File imageDir = new File("src/test/resources");
        ImageFormatMatchesChecker dockerImageFormatMatchesChecker = new DockerImageFormatMatchesChecker();

        assertFalse(dockerImageFormatMatchesChecker.applies(imageDir));
    }
}
