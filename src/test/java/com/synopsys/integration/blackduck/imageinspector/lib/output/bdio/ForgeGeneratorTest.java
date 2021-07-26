package com.synopsys.integration.blackduck.imageinspector.lib.output.bdio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ForgeGeneratorTest {

    @Test
    public void testBasic() {
        assertEquals("ubuntu", ForgeGenerator.createProjectForge("ubuntu").getName());
        assertEquals("@ubuntu", ForgeGenerator.createComponentForge("ubuntu").getName());
        assertEquals("@ubuntu", ForgeGenerator.createComponentForge("Ubuntu").getName());
        assertEquals("@centos", ForgeGenerator.createComponentForge("centos").getName());
        assertEquals("@alpine", ForgeGenerator.createComponentForge("alpine").getName());
        assertEquals("@opensuse", ForgeGenerator.createComponentForge("sles").getName());
        assertEquals("@opensuse", ForgeGenerator.createComponentForge("SLES").getName());
        assertEquals("@redhat", ForgeGenerator.createComponentForge("RHEL").getName());
        assertEquals("@redhat", ForgeGenerator.createComponentForge("rhel").getName());
        assertEquals("@fedora", ForgeGenerator.createComponentForge("fedora").getName());
        assertEquals("none", ForgeGenerator.createComponentForge(null).getName());
        assertEquals("@centos", ForgeGenerator.createComponentForge("amzn").getName());
    }

    @Test
    public void testOpenSuseVariants() {
        assertEquals("@opensuse", ForgeGenerator.createComponentForge("opensuse-leap").getName());
        assertEquals("@opensuse", ForgeGenerator.createComponentForge("openSUSE Leap").getName());
        assertEquals("@opensuse", ForgeGenerator.createComponentForge("openSUSE Tumbleweed").getName());
    }
}
