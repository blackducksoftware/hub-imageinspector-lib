package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry;


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

public class LayerEntriesTest {
  private static FileOperations fileOperations;

  @BeforeAll
  public static void setup() {
    fileOperations = Mockito.mock(FileOperations.class);
  }

  @Test
  public void testSymbolicLink() {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.getLinkName()).thenReturn("testLinkName");
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(true);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir);
    assertTrue(layerEntry instanceof LinkLayerEntry);
  }


  @Test
  public void testHardLink() {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.getLinkName()).thenReturn("testLinkName");
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(true);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir);
    assertTrue(layerEntry instanceof LinkLayerEntry);
  }


  @Test
  public void testFile() {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir);
    assertTrue(layerEntry instanceof FileDirLayerEntry);
  }



  @Test
  public void testWhiteOut() {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn(".wh.testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir);
    assertTrue(layerEntry instanceof WhiteOutFileLayerEntry);
  }



  @Test
  public void testWhiteOutOmittedDir() {
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn(".wh..wh..plnk");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir);
    assertTrue(layerEntry instanceof WhiteOutOmittedDirLayerEntry);
  }

}
