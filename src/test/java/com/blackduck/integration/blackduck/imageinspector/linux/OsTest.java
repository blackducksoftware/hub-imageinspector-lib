package com.blackduck.integration.blackduck.imageinspector.linux;



import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

public class OsTest {

    @Test
    public void testGetLinuxDistroNameUbuntu() {
        Os os = new Os();
        assertEquals("ubuntu", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/ubuntu")).get());
    }

    @Test
    public void testGetLinuxDistroNameCentos() {
        Os os = new Os();
        assertEquals("centos", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/centos")).get());
    }

    @Test
    public void testGetLinuxDistroNameRhel() {
        Os os = new Os();
        assertEquals("rhel", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/redhat")).get());
    }

    @Test
    public void testGetLinuxDistroNameFedora() {
        Os os = new Os();
        assertEquals("fedora", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/fedora")).get());
    }

    @Test
    public void testGetLinuxDistroNameOracleLinux() {
        Os os = new Os();
        assertEquals("oracle_linux", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/oracle-linux")).get());
    }

    @Test
    public void testGetLinuxDistroNameOracleLinuxOsRelease() {
        Os os = new Os();
        assertEquals("oracle_linux", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/oracle-linux-os-release")).get());
    }
}
