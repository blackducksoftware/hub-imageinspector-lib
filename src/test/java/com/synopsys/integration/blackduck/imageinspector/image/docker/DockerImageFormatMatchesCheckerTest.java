package com.synopsys.integration.blackduck.imageinspector.image.docker;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.ImageFormatMatchesChecker;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.image.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
