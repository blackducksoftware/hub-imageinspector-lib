package com.synopsys.integration.blackduck.imageinspector.image.oci;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.synopsys.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.synopsys.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;


public class OciImageDirectoryExtractorTest {
    private static String OCI_ALPINE_TEST_IMAGE_PATH = "src/test/resources/oci/alpine";
    private static String OCI_CENTOS_MINUS_VIM_BACULA_TEST_IMAGE_PATH = "src/test/resources/oci/centos_minus_vim_plus_bacula";


    @ParameterizedTest
    @MethodSource("testParseLayerArchivesProvider")
    public void testParseLayerArchives(String testImagePath, List<TypedArchiveFile> expectedArchiveList) throws IntegrationException {
        File ociImageDir = new File(testImagePath);
        CommonImageConfigParser configParser = new CommonImageConfigParser(new Gson());
        OciImageDirectoryExtractor extractor = new OciImageDirectoryExtractor(new Gson(), new FileOperations(), configParser);

        List<TypedArchiveFile> layerArchives = extractor.getLayerArchives(ociImageDir);
        Assertions.assertEquals(expectedArchiveList.size(), layerArchives.size());

        for (int i = 0; i < expectedArchiveList.size(); i++) {
            TypedArchiveFile expectedArchive = expectedArchiveList.get(i);
            TypedArchiveFile archive = layerArchives.get(i);
            String expectedFileName = expectedArchive.getFile().getName();
            ArchiveFileType expectedArchiveType = expectedArchive.getType();

            Assertions.assertEquals(expectedFileName, archive.getFile().getName());
            Assertions.assertEquals(expectedArchiveType, archive.getType());
        }
    }

    private static Stream<Arguments> testParseLayerArchivesProvider() {
        return Stream.of(
            arguments(
                OCI_ALPINE_TEST_IMAGE_PATH,
                Arrays.asList(
                    new TypedArchiveFile(ArchiveFileType.TAR_GZIPPED, new File("d3470daaa19c14ddf4ec500a3bb4f073fa9827aa4f19145222d459016ee9193e"))
                )
            ),
            arguments(
                OCI_CENTOS_MINUS_VIM_BACULA_TEST_IMAGE_PATH,
                Arrays.asList(
                    new TypedArchiveFile(ArchiveFileType.TAR_GZIPPED, new File("776bbd12a394aaddd5a05cf220efb6282461cfa6676f4118c1d90f073c06a192")),
                    new TypedArchiveFile(ArchiveFileType.TAR_GZIPPED, new File("d958a5c6553501f380698efa9b350aff8c948870b5d437c339ae7df5aba2ba5e"))
                )
            )
        );
    }


    @ParameterizedTest
    @MethodSource("testGetLayerMappingProvider")
    public void testGetLayerMapping(String imagePath, FullLayerMapping expected) throws IntegrationException {
        CommonImageConfigParser configParser = new CommonImageConfigParser(new Gson());
        OciImageDirectoryExtractor extractor = new OciImageDirectoryExtractor(new Gson(), new FileOperations(), configParser);
        File alpineOciImageDir = new File(imagePath);
        String testRepo = "testRepo";
        String testTag = "testTag";

        FullLayerMapping mapping = extractor.getLayerMapping(alpineOciImageDir, testRepo, testTag);
        ManifestLayerMapping manifestLayerMapping = mapping.getManifestLayerMapping();
        ManifestLayerMapping expectedManifestMapping = expected.getManifestLayerMapping();

        Assertions.assertEquals(testRepo, manifestLayerMapping.getImageName().get());
        Assertions.assertEquals(testTag, manifestLayerMapping.getTagName().get());
        Assertions.assertEquals(expectedManifestMapping.getPathToImageConfigFileFromRoot(), manifestLayerMapping.getPathToImageConfigFileFromRoot());

        List<String> externalIds = mapping.getLayerExternalIds();
        List<String> expectedExternalIds = expected.getLayerExternalIds();
        Assertions.assertEquals(expectedExternalIds.size(), externalIds.size());
        for (int index = 0; index < expectedExternalIds.size(); index++) {
            Assertions.assertEquals(expectedExternalIds.get(index), externalIds.get(index));
        }

        List<String> internalIds = manifestLayerMapping.getLayerInternalIds();
        List<String> expectedInternalIds = expectedManifestMapping.getLayerInternalIds();
        Assertions.assertEquals(expectedInternalIds.size(), internalIds.size());
        for (int index = 0; index < expectedInternalIds.size(); index++) {
            Assertions.assertEquals(expectedInternalIds.get(index), internalIds.get(index));
        }

    }

    private static Stream<Arguments> testGetLayerMappingProvider() {
        return Stream.of(
            arguments(
                OCI_ALPINE_TEST_IMAGE_PATH,
                new FullLayerMapping(
                    new ManifestLayerMapping(
                        "blobs/sha256/cdce9ebeb6e8364afeac430fe7a886ca89a90a5139bc3b6f40b5dbd0cf66391c",
                            Arrays.asList(
                            "sha256:d3470daaa19c14ddf4ec500a3bb4f073fa9827aa4f19145222d459016ee9193e")
                    ),
                    Arrays.asList(
                        "sha256:b2d5eeeaba3a22b9b8aa97261957974a6bd65274ebd43e1d81d0a7b8b752b116"
                    )
                )
            ),
            arguments(
                OCI_CENTOS_MINUS_VIM_BACULA_TEST_IMAGE_PATH,
                new FullLayerMapping(
                    new ManifestLayerMapping(
                        "blobs/sha256/7e48f272d1c0b356a618d99554a9233c0b1dff1491b7bfd42c55d9eeddab3ab7",
                        Arrays.asList(
                            "sha256:776bbd12a394aaddd5a05cf220efb6282461cfa6676f4118c1d90f073c06a192",
                            "sha256:d958a5c6553501f380698efa9b350aff8c948870b5d437c339ae7df5aba2ba5e"
                        )
                    ),
                    Arrays.asList(
                        "sha256:0e07d0d4c60c0a54ad297763c829584b15d1a4a848bf21fb69dc562feee5bf11",
                        "sha256:a43c72846ed18b6c373821c3aaa1ba2eaafc273b8b7d0a03cc08b94909faa525"
                    )
                )
            )
        );
    }

}
