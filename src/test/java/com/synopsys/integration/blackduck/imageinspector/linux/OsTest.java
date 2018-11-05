package test.java.com.synopsys.integration.blackduck.imageinspector.linux;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.io.File;

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
}
