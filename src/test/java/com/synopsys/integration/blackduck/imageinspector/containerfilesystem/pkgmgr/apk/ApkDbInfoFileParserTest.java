package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;

public class ApkDbInfoFileParserTest {
    @Test
    public void testParseDbInfoFile() throws IOException {
        File dbInfoFile = new File("src/test/resources/dbRelationshipParsing/apkInstalled.txt");
        ApkDbInfoFileParser apkDbInfoFileParser = new ApkDbInfoFileParser();
        List<String> lines = Files.readAllLines(dbInfoFile.toPath());
        DbRelationshipInfo dbRelationshipInfo = apkDbInfoFileParser.parseDbRelationshipInfoFromFile(lines);
        Map<String, List<String>> compNamesToDependencies = dbRelationshipInfo.getCompNamesToDependencies();
        Map<String, String> providedBinariesToCompNames = dbRelationshipInfo.getProvidedBinariesToCompNames();

        Assertions.assertNull(compNamesToDependencies.get("musl"));

        List<String> busyBoxDeps = compNamesToDependencies.get("busybox");
        Assertions.assertTrue(busyBoxDeps.contains("so:libc.musl-x86_64.so.1"));

        List<String> alpineBaseeLayoutDeps = compNamesToDependencies.get("alpine-baselayout");
        Assertions.assertTrue(alpineBaseeLayoutDeps.contains("musl"));

        Assertions.assertEquals("musl", providedBinariesToCompNames.get("so:libc.musl-x86_64.so.1"));
        Assertions.assertEquals("busybox", providedBinariesToCompNames.get("/bin/sh"));
        Assertions.assertEquals("busybox", providedBinariesToCompNames.get("cmd:busybox"));
        Assertions.assertEquals("busybox", providedBinariesToCompNames.get("cmd:sh"));
    }
}
