package com.blackduck.integration.blackduck.imageinspector.image.common;

import com.blackduck.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.ArchiveFileType;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.ImageLayerArchiveExtractor;
import com.blackduck.integration.blackduck.imageinspector.image.common.archive.TypedArchiveFile;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ImageLayerApplierTest {

    @Test
    void test() throws WrongInspectorOsException, IOException {
        FileOperations fileOperations = Mockito.mock(FileOperations.class);
        ImageLayerArchiveExtractor imageLayerArchiveExtractor = Mockito.mock(ImageLayerArchiveExtractor.class);
        ImageLayerApplier imageLayerApplier = new ImageLayerApplier(fileOperations, imageLayerArchiveExtractor);
        File destinationDir = Mockito.mock(File.class);
        TypedArchiveFile layerTar = Mockito.mock(TypedArchiveFile.class);
        File layerTarFile = Mockito.mock(File.class);
        Mockito.when(layerTar.getFile()).thenReturn(layerTarFile);
        Mockito.when(layerTar.getType()).thenReturn(ArchiveFileType.TAR);
        File fileToRemove = Mockito.mock(File.class);
        Mockito.when(fileToRemove.isDirectory()).thenReturn(false);
        File dirToRemove = Mockito.mock(File.class);
        Mockito.when(dirToRemove.isDirectory()).thenReturn(true);
        List<File> filesToRemove = Arrays.asList(fileToRemove, dirToRemove);
        Mockito.when(imageLayerArchiveExtractor.extractLayerTarToDir(fileOperations, layerTarFile, destinationDir)).thenReturn(filesToRemove);

        imageLayerApplier.applyLayer(destinationDir, layerTar);

        Mockito.verify(fileOperations).deleteQuietly(fileToRemove);
        Mockito.verify(fileOperations).deleteDirectory(dirToRemove);
    }
}
