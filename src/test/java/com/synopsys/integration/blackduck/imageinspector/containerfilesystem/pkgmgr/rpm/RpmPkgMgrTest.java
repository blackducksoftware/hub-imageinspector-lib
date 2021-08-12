package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.rpm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.synopsys.integration.exception.IntegrationException;

public class RpmPkgMgrTest {

    // TODO there are tests like this in BdioGeneratorTest; pick one!

    @Test
    public void test() throws IntegrationException {
        final PkgMgr pkgMgr = new RpmPkgMgr(new Gson(), new FileOperations());
        final String[] rpmOutputLines = {
            "{ epoch: \"(none)\", name: \"basesystem\", version: \"10.0-7.el7.centos\", arch: \"noarch\" }",
            "{ epoch: \"2\", name: \"libpng\", version: \"1.5.13-7.el7_2\", arch: \"x86_64\" }"
        };

        final List<ComponentDetails> comps = pkgMgr.extractComponentsFromPkgMgrOutput(new File("test"), "centos", rpmOutputLines);

        assertEquals(2, comps.size());
        assertEquals("basesystem/10.0-7.el7.centos/noarch", comps.get(0).getExternalId());
        assertEquals("10.0-7.el7.centos", comps.get(0).getVersion());
        assertEquals("noarch", comps.get(0).getArchitecture());

        assertEquals("libpng/2:1.5.13-7.el7_2/x86_64", comps.get(1).getExternalId());
        assertEquals("2:1.5.13-7.el7_2", comps.get(1).getVersion());
        assertEquals("libpng", comps.get(1).getName());
        assertEquals("centos", comps.get(1).getLinuxDistroName());
    }
}
