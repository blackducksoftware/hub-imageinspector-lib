package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry.WhiteOutFileLayerEntry;
import java.io.File;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.Test;
import org.mockito.Mockito;

public class WhiteOutFileLayerEntryTest {

  @Test
  public void testValid() {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn(".wh.testWhitedOutFileName");
    final File layerOutputDir = new File("test/output");
    final WhiteOutFileLayerEntry layerEntry = new WhiteOutFileLayerEntry(archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    assertEquals(Optional.empty(), fileToRemove);
  }

  @Test
  public void testInvalid() {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testInvalidWhitedOutFileName");
    final File layerOutputDir = new File("test/output");
    final WhiteOutFileLayerEntry layerEntry = new WhiteOutFileLayerEntry(archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    assertEquals(Optional.empty(), fileToRemove);
  }
}
