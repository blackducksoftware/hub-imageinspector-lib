package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.CommonRelationshipPopulater;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;

public class CommonRelationshipPopulaterTest {
    @Test
    public void testPopulateRelationshipInfo() {
        Map<String, List<String>> compNamesToDependencies = new HashMap<>();
        compNamesToDependencies.put("busybox", Arrays.asList("so:libc.musl-x86_64.so.1"));
        compNamesToDependencies.put("alpine-layout", Arrays.asList("musl"));

        Map<String, String> providedBinariesToCompNames = new HashMap<>();
        providedBinariesToCompNames.put("cmd:busybox=1.34.1-r3", "busybox");
        providedBinariesToCompNames.put("so:libc.musl-x86_64.so.1", "musl");

        DbRelationshipInfo dbRelationshipInfo = new DbRelationshipInfo(compNamesToDependencies, providedBinariesToCompNames);

        ComponentDetails musl = new ComponentDetails("musl", null, null, null, null);
        ComponentDetails busybox = new ComponentDetails("busybox", null, null, null, null);
        ComponentDetails alpineLayout = new ComponentDetails("alpine-layout", null, null, null, null);
        List<ComponentDetails> components = Arrays.asList(musl, busybox, alpineLayout);

        CommonRelationshipPopulater relationshipPopulater = new CommonRelationshipPopulater(dbRelationshipInfo);
        relationshipPopulater.populateRelationshipInfo(components);

        Assertions.assertTrue(busybox.getDependencies().contains(musl));
        Assertions.assertTrue(alpineLayout.getDependencies().contains(musl));
    }
}
