package com.synopsys.integration.blackduck.imageinspector.imageformat.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry.LayerEntry;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry.WhiteOutOmittedDirLayerEntry;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class WhiteOutOmittedDirLayerEntryTest {
  private static FileOperations fileOperations;

  @BeforeAll
  public static void setup() {
    fileOperations = Mockito.mock(FileOperations.class);
  }

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
