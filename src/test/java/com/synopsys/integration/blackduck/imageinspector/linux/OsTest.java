package test.java.com.synopsys.integration.blackduck.imageinspector.linux;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.io.File;

public class OsTest {

    @Test
    public void testGetLinuxDistroName() {
        com.synopsys.integration.blackduck.imageinspector.linux.Os os = new com.synopsys.integration.blackduck.imageinspector.linux.Os();
        assertEquals("ubuntu", os.getLinxDistroName(new File("src/test/resources/os-release")).get());
    }
}
