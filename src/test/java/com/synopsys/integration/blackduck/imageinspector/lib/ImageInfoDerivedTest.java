package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.bdio.model.BdioProject;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.PackageManagerToImageInspectorOsMapping;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;

public class ImageInfoDerivedTest {

  @Test
  public void test() {
    ImagePkgMgrDatabase pkgMgrDb = new ImagePkgMgrDatabase(new File("src/test/resources/imageDir/etc/apk"),
        PackageManagerEnum.APK);
    final File targetImageFileSystemRootDir = new File("src/test/resources/imageDir");
    final ContainerFileSystem containerFileSystem = new ContainerFileSystem(targetImageFileSystemRootDir);
    ContainerFileSystemWithPkgMgrDb parsed = new ContainerFileSystemWithPkgMgrDb(containerFileSystem,
        pkgMgrDb, "alpine", new ApkPkgMgr(new FileOperations()));

    assertEquals("imageDir", parsed.getTargetImageFileSystem().getTargetImageFileSystemFull().getName());
    assertEquals("alpine", parsed.getLinuxDistroName());
    assertEquals(PackageManagerEnum.APK, parsed.getImagePkgMgrDatabase().getPackageManager());
    assertEquals("apk", parsed.getImagePkgMgrDatabase().getExtractedPackageManagerDirectory().getName());

    // TODO also test the imageComponentHierarchy part?
    ImageInfoDerived derived = new ImageInfoDerived(parsed, null);
    derived.setCodeLocationName("testCodelocationName");
    SimpleBdioDocument bdioDoc = new SimpleBdioDocument();
    bdioDoc.setProject(new BdioProject());
    bdioDoc.getProject().name = "testProjectName";
    assertEquals("testProjectName", bdioDoc.getProject().name);
    derived.setBdioDocument(bdioDoc);
    derived.setFinalProjectName("testFinalProjectName");
    derived.setFinalProjectVersionName("testFinalProjectVersionName");
    ManifestLayerMapping mapping = new ManifestLayerMapping("testImageName", "testTagName", "testConfig", Arrays
        .asList("testLayer"));
    FullLayerMapping fullLayerMapping = new FullLayerMapping(mapping, new ArrayList<>());
    derived.setFullLayerMapping(fullLayerMapping);

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
