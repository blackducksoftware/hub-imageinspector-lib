package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class LinkLayerEntryTest {
  private static FileOperations fileOperations;

  @BeforeAll
  public static void setup() {
    fileOperations = Mockito.mock(FileOperations.class);
  }

  @Test
  public void testSymbolicLink() throws IOException {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.getLinkName()).thenReturn("testLinkName");
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(true);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = new LinkLayerEntry(fileOperations, archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    Path startLinkPath = new File(new File("test/output/testFileName").getAbsolutePath()).toPath();
    Path endLinkPath = new File(new File("test/output/testLinkName").getAbsolutePath()).toPath();
    Mockito.verify(fileOperations).createSymbolicLink(startLinkPath, endLinkPath);
    assertEquals(Optional.empty(), fileToRemove);
  }

  @Test
  public void testRelSymbolicLink() throws IOException {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.getLinkName()).thenReturn("../sisterDir/testFileName");
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(true);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = new LinkLayerEntry(fileOperations, archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    Path startLinkPath = new File(new File("test/output/testFileName").getAbsolutePath()).toPath();
    Path endLinkPath = new File("../sisterDir/testFileName").toPath();
    Mockito.verify(fileOperations).createSymbolicLink(startLinkPath, endLinkPath);
    assertEquals(Optional.empty(), fileToRemove);
  }

  @Test
  public void testHardLink() throws IOException {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.getLinkName()).thenReturn("testLinkName");
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(true);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = new LinkLayerEntry(fileOperations, archiveEntry, layerOutputDir);
    Optional<File> fileToRemove = layerEntry.process();
    Path startLinkPath = new File(new File("test/output/testFileName").getAbsolutePath()).toPath();
    Path endLinkPath = new File("test/output/testLinkName").toPath();
    Mockito.verify(fileOperations).createLink(startLinkPath, endLinkPath);
    assertEquals(Optional.empty(), fileToRemove);
  }
}
