package com.blackduck.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import com.blackduck.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.blackduck.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystem;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.ContainerFileSystemWithPkgMgrDb;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.ImagePkgMgrDatabase;
import com.blackduck.integration.blackduck.imageinspector.image.common.FullLayerMapping;
import com.blackduck.integration.blackduck.imageinspector.image.common.ImageInfoDerived;
import com.blackduck.integration.blackduck.imageinspector.image.common.ManifestLayerMapping;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.BdioProject;
import com.blackduck.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.PackageManagerToImageInspectorOsMapping;
import com.blackduck.integration.blackduck.imageinspector.linux.FileOperations;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk.ApkPkgMgr;

public class ImageInfoDerivedTest {

  @Test
  public void test() {
    ImagePkgMgrDatabase pkgMgrDb = new ImagePkgMgrDatabase(new File("src/test/resources/imageDir/etc/apk"),
        PackageManagerEnum.APK);
    final File targetImageFileSystemRootDir = new File("src/test/resources/imageDir");
    final ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir);
    ContainerFileSystemWithPkgMgrDb containerFileSystemWithPkgMgrDb = new ContainerFileSystemWithPkgMgrDb(containerFileSystem,
        pkgMgrDb, "alpine", new ApkPkgMgr(new FileOperations()));

    assertEquals("imageDir", containerFileSystemWithPkgMgrDb.getContainerFileSystem().getTargetImageFileSystemFull().getName());
    assertEquals("alpine", containerFileSystemWithPkgMgrDb.getLinuxDistroName());
    assertEquals(PackageManagerEnum.APK, containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getPackageManager());
    assertEquals("apk", containerFileSystemWithPkgMgrDb.getImagePkgMgrDatabase().getExtractedPackageManagerDirectory().getName());

    // TODO also test the imageComponentHierarchy part?
    ManifestLayerMapping mapping = new ManifestLayerMapping("testImageName", "testTagName", "testConfig", Arrays
            .asList("testLayer"));
    FullLayerMapping fullLayerMapping = new FullLayerMapping(mapping, new ArrayList<>());
    SimpleBdioDocument bdioDoc = new SimpleBdioDocument();
    bdioDoc.setProject(new BdioProject());
    bdioDoc.getProject().name = "testProjectName";
    ImageInfoDerived derived = new ImageInfoDerived(fullLayerMapping, containerFileSystemWithPkgMgrDb, null, "testCodelocationName",
            "testFinalProjectName", "testFinalProjectVersionName", bdioDoc);

    assertEquals("testProjectName", bdioDoc.getProject().name);
    assertEquals("testLayer", derived.getFullLayerMapping().getManifestLayerMapping().getLayerInternalIds().get(0));
    assertEquals("testCodelocationName", derived.getCodeLocationName());
    assertEquals("testFinalProjectName", derived.getFinalProjectName());
    assertEquals("testFinalProjectVersionName", derived.getFinalProjectVersionName());
    assertEquals("testProjectName", derived.getBdioDocument().getProject().name);
    assertEquals(
        ImageInspectorOsEnum.ALPINE, PackageManagerToImageInspectorOsMapping
            .getImageInspectorOs(derived.getImageInfoParsed().getImagePkgMgrDatabase().getPackageManager()));
  }

}
