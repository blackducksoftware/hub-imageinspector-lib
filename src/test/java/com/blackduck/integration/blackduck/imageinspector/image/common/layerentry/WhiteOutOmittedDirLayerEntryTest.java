package com.blackduck.integration.blackduck.imageinspector.image.common.layerentry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class WhiteOutOmittedDirLayerEntryTest {

  @Test
  public void testValid() throws IOException {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = new WhiteOutOmittedDirLayerEntry(archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    assertEquals("output", fileToRemove.get().getName());
  }
}
