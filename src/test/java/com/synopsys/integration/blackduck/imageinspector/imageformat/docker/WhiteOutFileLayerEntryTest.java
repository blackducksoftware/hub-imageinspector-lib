package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry.WhiteOutFileLayerEntry;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

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
    final WhiteOutFileLayerEntry layerEntry = new WhiteOutFileLayerEntry(fileOperations, archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    assertEquals(Optional.empty(), fileToRemove);
    Mockito.verify(fileOperations).removeFile(new File("test/output/testWhitedOutFileName"));
  }

  @Test
  public void testInvalid() {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testInvalidWhitedOutFileName");
    final File layerOutputDir = new File("test/output");
    final WhiteOutFileLayerEntry layerEntry = new WhiteOutFileLayerEntry(fileOperations, archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    assertEquals(Optional.empty(), fileToRemove);
  }
}
