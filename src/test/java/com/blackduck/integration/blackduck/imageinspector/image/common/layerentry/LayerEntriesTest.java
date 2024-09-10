package com.blackduck.integration.blackduck.imageinspector.image.common.layerentry;


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;

public class LayerEntriesTest {

  @Test
  public void testSymbolicLink() throws IOException {
    FileOperations fileOperations = Mockito.mock(FileOperations.class);
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.getLinkName()).thenReturn("testLinkName");
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(true);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();

    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir, fileDeleter);
    layerEntry.processFiles();

    assertTrue(layerEntry instanceof LinkLayerEntry);
    Path expectedFrom = Paths.get(new File("test/output/testFileName").getAbsolutePath());
    Path expectedTo = Paths.get("testLinkName");
    Mockito.verify(fileOperations).createSymbolicLink(expectedFrom, expectedTo);
  }

  @Test
  public void testSymbolicLinkAbsolutePath() throws IOException {
    FileOperations fileOperations = Mockito.mock(FileOperations.class);
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);

    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.getLinkName()).thenReturn("/testLinkName");
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(true);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();
    final File layerOutputDir = Paths.get(new File("test/output").getAbsolutePath()).toFile();

    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir, fileDeleter);
    layerEntry.processFiles();

    assertTrue(layerEntry instanceof LinkLayerEntry);
    Path expectedFrom = Paths.get(new File("test/output/testFileName").getAbsolutePath());
    Path expectedTo = Paths.get("testLinkName");
    Mockito.verify(fileOperations).createSymbolicLink(expectedFrom, expectedTo);
  }


  @Test
  public void testHardLink() throws IOException {
    FileOperations fileOperations = Mockito.mock(FileOperations.class);
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.getLinkName()).thenReturn("testLinkName");
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(true);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();

    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir, fileDeleter);
    layerEntry.processFiles();

    assertTrue(layerEntry instanceof LinkLayerEntry);
    Path expectedFrom = Paths.get(new File("test/output/testFileName").getAbsolutePath());
    Path expectedTo = Paths.get("test/output/testLinkName");
    Mockito.verify(fileOperations).createLink(expectedFrom, expectedTo);
  }


  @Test
  public void testFile() throws IOException {
    FileOperations fileOperations = Mockito.mock(FileOperations.class);
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn("testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();

    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir, fileDeleter);
    layerEntry.processFiles();

    assertTrue(layerEntry instanceof FileDirLayerEntry);
    Mockito.verify(fileOperations).copy(Mockito.any(InputStream.class), Mockito.any(OutputStream.class));
  }



  @Test
  public void testWhiteOut() {
    FileOperations fileOperations = Mockito.mock(FileOperations.class);
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn(".wh.testFileName");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();
    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir, fileDeleter);
    assertTrue(layerEntry instanceof WhiteOutFileLayerEntry);
  }



  @Test
  public void testWhiteOutOmittedDir() {
    FileOperations fileOperations = Mockito.mock(FileOperations.class);
    final TarArchiveEntry archiveEntry = Mockito.mock(TarArchiveEntry.class);
    Mockito.when(archiveEntry.getName()).thenReturn(".wh..wh..plnk");
    Mockito.when(archiveEntry.isFile()).thenReturn(true);
    Mockito.when(archiveEntry.isSymbolicLink()).thenReturn(false);
    Mockito.when(archiveEntry.isLink()).thenReturn(false);
    final TarArchiveInputStream layerInputStream = Mockito.mock(TarArchiveInputStream.class);
    final File layerOutputDir = new File("test/output");
    final LowerLayerFileDeleter fileDeleter = new LowerLayerFileDeleter();
    final LayerEntry layerEntry = LayerEntries.createLayerEntry(fileOperations, layerInputStream, archiveEntry, layerOutputDir, fileDeleter);
    assertTrue(layerEntry instanceof WhiteOutOmittedDirLayerEntry);
  }

}
