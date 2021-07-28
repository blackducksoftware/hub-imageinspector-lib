package com.synopsys.integration.blackduck.imageinspector.image.common.layerentry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class WhiteOutFileLayerEntryTest {
  private static FileOperations fileOperations;

  @BeforeAll
  public static void setup() {
    fileOperations = Mockito.mock(FileOperations.class);
  }

  @Test
  public void testValid() throws IOException {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn(".wh.testWhitedOutFileName");
    final File layerOutputDir = new File("test/output");
    final LowerLayerFileDeleter fileDeleter = Mockito.mock(LowerLayerFileDeleter.class);
    final LayerEntry layerEntry = new WhiteOutFileLayerEntry(fileOperations, archiveEntry, layerOutputDir, fileDeleter);
    Optional<File> fileToRemove = layerEntry.process();
    assertEquals(Optional.empty(), fileToRemove);
    Mockito.verify(fileDeleter).deleteFilesAddedByLowerLayers(new File("test/output/testWhitedOutFileName"));
  }

  @Test
  public void testInvalid() throws IOException {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testInvalidWhitedOutFileName");
    final File layerOutputDir = new File("test/output");
    final LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();
    final LayerEntry layerEntry = new WhiteOutFileLayerEntry(fileOperations, archiveEntry, layerOutputDir, fileDeleter);
    Optional<File> fileToRemove = layerEntry.process();
    assertEquals(Optional.empty(), fileToRemove);
  }
}
