package com.synopsys.integration.blackduck.imageinspector.image.docker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.image.docker.manifest.DockerManifestFactory;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class DockerImageDirectoryExtractorTest {

    @Test
    void test() throws IOException, IntegrationException {
        File imageDir = new File("src/test/resources/mockDockerTarContents");
        Gson gson = new Gson();
        FileOperations fileOperations = new FileOperations();
        CommonImageConfigParser commonImageConfigParser = new CommonImageConfigParser(gson);
        DockerManifestFactory dockerManifestFactory = new DockerManifestFactory();
        DockerImageDirectoryExtractor extractor = new DockerImageDirectoryExtractor(gson, fileOperations, commonImageConfigParser, dockerManifestFactory);

        List<TypedArchiveFile> typedArchiveFiles = extractor.getLayerArchives(imageDir, null, null);

        assertEquals(1, typedArchiveFiles.size());
        assertEquals(ArchiveFileType.TAR, typedArchiveFiles.get(0).getType());
        assertEquals("layer.tar", typedArchiveFiles.get(0).getFile().getName());

        FullLayerMapping fullLayerMapping = extractor.getLayerMapping(imageDir, null, null);

        assertEquals("sha256:503e53e365f34399c4d58d8f4e23c161106cfbce4400e3d0a0357967bad69390", fullLayerMapping.getLayerExternalId(0));
    }

    @Test
    void testNonExistentRepoTag() throws IntegrationException {
        File imageDir = new File("src/test/resources/mockDockerTarContents");
        Gson gson = new Gson();
        FileOperations fileOperations = new FileOperations();
        CommonImageConfigParser dockerImageConfigParser = new CommonImageConfigParser(gson);
        DockerManifestFactory dockerManifestFactory = new DockerManifestFactory();
        DockerImageDirectoryExtractor extractor = new DockerImageDirectoryExtractor(gson, fileOperations, dockerImageConfigParser, dockerManifestFactory);

        List<TypedArchiveFile> typedArchiveFiles = extractor.getLayerArchives(imageDir, "", "");

        assertEquals(1, typedArchiveFiles.size());
        assertEquals(ArchiveFileType.TAR, typedArchiveFiles.get(0).getType());
        assertEquals("layer.tar", typedArchiveFiles.get(0).getFile().getName());

        try {
            extractor.getLayerMapping(imageDir, "nonexistentrepo", "nonexistenttag");
            fail("Expected exception");
        } catch (IntegrationException e) {
        }
    }
}
