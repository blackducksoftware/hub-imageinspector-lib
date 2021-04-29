package test.java.com.synopsys.integration.blackduck.imageinspector.linux;



import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

public class OsTest {

    @Test
    public void testGetLinuxDistroNameUbuntu() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("ubuntu", os.getLinxDistroName(new File("src/test/resources/os-release")).get());
    }

    @Test
    public void testGetLinuxDistroNameCentos() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("centos", os.getLinxDistroName(new File("src/test/resources/osdetection/centos/redhat-release")).get());
    }

    @Test
    public void testGetLinuxDistroNameRhel() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("rhel", os.getLinxDistroName(new File("src/test/resources/osdetection/redhat/redhat-release")).get());
    }

    @Test
    public void testGetLinuxDistroNameFedora() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("fedora", os.getLinxDistroName(new File("src/test/resources/osdetection/fedora/redhat-release")).get());
    }
}
