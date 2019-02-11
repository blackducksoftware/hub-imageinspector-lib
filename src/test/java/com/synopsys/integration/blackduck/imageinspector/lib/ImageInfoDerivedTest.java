package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.synopsys.integration.bdio.model.BdioProject;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.api.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import java.io.File;
import java.util.Arrays;
import org.junit.Test;

public class ImageInfoDerivedTest {

  @Test
  public void test() {
    ImagePkgMgrDatabase pkgMgrDb = new ImagePkgMgrDatabase(new File("src/test/resources/imageDir/etc/apk"),
        PackageManagerEnum.APK);
    ImageInfoParsed parsed = new ImageInfoParsed(new File("src/test/resources/imageDir"),
        pkgMgrDb, "alpine");

    assertEquals("imageDir", parsed.getFileSystemRootDir().getName());
    assertEquals("alpine", parsed.getLinuxDistroName());
    assertEquals(PackageManagerEnum.APK, parsed.getPkgMgr().getPackageManager());
    assertEquals("apk", parsed.getPkgMgr().getExtractedPackageManagerDirectory().getName());

    ImageInfoDerived derived = new ImageInfoDerived(parsed);
    derived.setCodeLocationName("testCodelocationName");
    SimpleBdioDocument bdioDoc = new SimpleBdioDocument();
    bdioDoc.project = new BdioProject();
    bdioDoc.project.name = "testProjectName";
    assertEquals("testProjectName", bdioDoc.project.name);
    derived.setBdioDocument(bdioDoc);
    derived.setFinalProjectName("testFinalProjectName");
    derived.setFinalProjectVersionName("testFinalProjectVersionName");
    ManifestLayerMapping mapping = new ManifestLayerMapping("testImageName", "testTagName", "testConfig", Arrays
        .asList("testLayer"));
    derived.setManifestLayerMapping(mapping);

    assertEquals("testLayer", derived.getManifestLayerMapping().getLayers().get(0));
    assertEquals("testCodelocationName", derived.getCodeLocationName());
    assertEquals("testFinalProjectName", derived.getFinalProjectName());
    assertEquals("testFinalProjectVersionName", derived.getFinalProjectVersionName());
    assertEquals("testProjectName", derived.getBdioDocument().project.name);
    assertEquals(
        OperatingSystemEnum.ALPINE, derived.getImageInfoParsed().getPkgMgr().getPackageManager().getInspectorOperatingSystem());
  }

}
