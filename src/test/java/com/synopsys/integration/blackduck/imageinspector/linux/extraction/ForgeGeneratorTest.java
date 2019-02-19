package com.synopsys.integration.blackduck.imageinspector.linux.extraction;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ForgeGeneratorTest {

    @Test
    public void test() {
        assertEquals("ubuntu", ForgeGenerator.createProjectForge("ubuntu").getName());
        assertEquals("@ubuntu", ForgeGenerator.createComponentForge("ubuntu").getName());
        assertEquals("@ubuntu", ForgeGenerator.createComponentForge("Ubuntu").getName());
        assertEquals("@centos", ForgeGenerator.createComponentForge("centos").getName());
        assertEquals("@alpine", ForgeGenerator.createComponentForge("alpine").getName());
        assertEquals("@opensuse", ForgeGenerator.createComponentForge("sles").getName());
        assertEquals("@opensuse", ForgeGenerator.createComponentForge("SLES").getName());
        assertEquals("@redhat", ForgeGenerator.createComponentForge("RHEL").getName());
        assertEquals("@redhat", ForgeGenerator.createComponentForge("rhel").getName());
        assertEquals("none", ForgeGenerator.createComponentForge(null).getName());
    }
}
