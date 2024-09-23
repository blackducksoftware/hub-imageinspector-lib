package com.blackduck.integration.blackduck.imageinspector.image.oci;

import com.google.gson.Gson;
import com.blackduck.integration.blackduck.imageinspector.image.common.ImageFormatMatchesChecker;
import com.blackduck.integration.blackduck.imageinspector.image.docker.DockerImageFormatMatchesChecker;
import com.blackduck.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OciImageFormatMatchesCheckerTest {
    @Test
    void testTrue() throws IntegrationException {
        File imageDir = new File("src/test/resources/mockOciTarContents");
        OciLayoutParser ociLayoutParser = new OciLayoutParser(new Gson());
        ImageFormatMatchesChecker ociImageFormatMatchesChecker = new OciImageFormatMatchesChecker(ociLayoutParser);

        assertTrue(ociImageFormatMatchesChecker.applies(imageDir));
    }

    @Test
    void testFalse() throws IntegrationException {
        File imageDir = new File("src/test/resources");
        ImageFormatMatchesChecker ociImageFormatMatchesChecker = new DockerImageFormatMatchesChecker();

        assertFalse(ociImageFormatMatchesChecker.applies(imageDir));
    }
}
