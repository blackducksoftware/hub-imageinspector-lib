package test.java.com.synopsys.integration.blackduck.imageinspector.linux;



import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

public class OsTest {

    @Test
    public void testGetLinuxDistroNameUbuntu() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("ubuntu", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/ubuntu")).get());
    }

    @Test
    public void testGetLinuxDistroNameCentos() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("centos", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/centos")).get());
    }

    @Test
    public void testGetLinuxDistroNameRhel() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("rhel", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/redhat")).get());
    }

    @Test
    public void testGetLinuxDistroNameFedora() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("fedora", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/fedora")).get());
    }

    @Test
    public void testGetLinuxDistroNameOracleLinux() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("oracle_linux", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/oracle-linux")).get());
    }

    @Test
    public void testGetLinuxDistroNameOracleLinuxOsRelease() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("oracle_linux", os.getLinuxDistroNameFromEtcDir(new File("src/test/resources/osdetection/oracle-linux-os-release")).get());
    }
}
