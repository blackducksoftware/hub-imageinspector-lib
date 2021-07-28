package com.synopsys.integration.blackduck.imageinspector.image.common.layerentry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class FileDirLayerEntryTest {
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
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final LayerEntry layerEntry = new FileDirLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    assertEquals(Optional.empty(), fileToRemove);
    Mockito.verify(fileOperations).copy(Mockito.any(InputStream.class), Mockito.any(OutputStream.class));
  }
}
