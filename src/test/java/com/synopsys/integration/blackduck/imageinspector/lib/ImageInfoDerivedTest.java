package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import java.io.File;
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
  }

}
