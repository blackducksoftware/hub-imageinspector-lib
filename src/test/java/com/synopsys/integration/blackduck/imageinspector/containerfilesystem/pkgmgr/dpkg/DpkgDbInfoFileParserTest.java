package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.dpkg;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;

public class DpkgDbInfoFileParserTest {
    @Test
    public void testParseDbInfoFile() {
        File dbInfoFile = new File("src/test/resources/dbRelationshipParsing/dpkgStatus.txt");
        DpkgDbInfoFileParser dpkgDbInfoFileParser = new DpkgDbInfoFileParser();
        DbRelationshipInfo dbRelationshipInfo = dpkgDbInfoFileParser.parseDbRelationshipInfoFromFile(dbInfoFile);
        Map<String, List<String>> compNamesToDependencies = dbRelationshipInfo.getCompNamesToDependencies();
        Map<String, String> providedBinariesToCompNames = dbRelationshipInfo.getProvidedBinariesToCompNames();

        Assertions.assertNull(compNamesToDependencies.get("base-files"));

        List<String> bashDeps = compNamesToDependencies.get("bash");
        Assertions.assertTrue(bashDeps.contains("base-files"));

        List<String> coreutilsDeps = compNamesToDependencies.get("coreutils");
        Assertions.assertTrue(coreutilsDeps.contains("libc6"));

        Assertions.assertEquals("base-files", providedBinariesToCompNames.get("base"));
        Assertions.assertEquals("base-files", providedBinariesToCompNames.get("libc6"));
    }
}
