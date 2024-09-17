package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.rpm;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.blackduck.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.blackduck.integration.exception.IntegrationException;

public class RpmRelationshipPopulatorTest {
    @Test
    public void testPopulateRelationships() throws IntegrationException, UnsupportedEncodingException {
        CmdExecutor cmdExecutor = Mockito.mock(CmdExecutor.class);
        Long timeout = 120000L;
        Mockito.when(cmdExecutor.executeCommand(Arrays.asList("rpm", "-qR", "dhcp-client"), timeout)).thenReturn(new String[]{ "gawk", "systemd" });
        Mockito.when(cmdExecutor.executeCommand(Arrays.asList("rpm", "-qR", "systemd"), timeout)).thenReturn(new String[]{ "util-linux", "so:libc" });
        Mockito.when(cmdExecutor.executeCommand(Arrays.asList("rpm", "-qR", "gawk"), timeout)).thenReturn(new String[0]);
        Mockito.when(cmdExecutor.executeCommand(Arrays.asList("rpm", "-qR", "util-linux"), timeout)).thenReturn(new String[0]);

        ComponentDetails dhcpClient = new ComponentDetails("dhcp-client", null, null, null, null);
        ComponentDetails gawk = new ComponentDetails("gawk", null, null, null, null);
        ComponentDetails systemd = new ComponentDetails("systemd", null, null, null, null);
        ComponentDetails utilLinux = new ComponentDetails("util-linux", null, null, null, null);
        List<ComponentDetails> comps = Arrays.asList(dhcpClient, gawk, systemd, utilLinux);

        RpmRelationshipPopulater relationshipPopulater = new RpmRelationshipPopulater(cmdExecutor);
        relationshipPopulater.populateRelationshipInfo(comps);

        Assertions.assertTrue(dhcpClient.getDependencies().contains(gawk));
        Assertions.assertTrue(dhcpClient.getDependencies().contains(systemd));
        Assertions.assertTrue(systemd.getDependencies().contains(utilLinux));
        Assertions.assertEquals(1, systemd.getDependencies().size());
    }
}
