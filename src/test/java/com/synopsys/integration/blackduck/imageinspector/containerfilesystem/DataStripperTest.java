package com.synopsys.integration.blackduck.imageinspector.containerfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class DataStripperTest {
    
    @Test
    void stripEpochFromVersionTest() {
        assertEquals(null, DataStripper.stripEpochFromVersion(null));
        assertEquals("", DataStripper.stripEpochFromVersion(""));
        assertEquals("1:0:", DataStripper.stripEpochFromVersion("1:0:"));
        assertEquals("0:", DataStripper.stripEpochFromVersion("0:0:"));
        assertEquals(" ", DataStripper.stripEpochFromVersion(" "));
        assertEquals("1.62.0-r5", DataStripper.stripEpochFromVersion("1.62.0-r5"));
        assertEquals("1.62.0-r5", DataStripper.stripEpochFromVersion("0:1.62.0-r5"));
    }
    
    @Test
    void stripEpochFromExternalIdTest() {
        assertEquals("name/:7.0.0", DataStripper.stripEpochFromExternalId("name/:7.0.0"));
        assertEquals("/7.0.0", DataStripper.stripEpochFromExternalId("/7.0.0"));
        assertEquals("name/7.0.0", DataStripper.stripEpochFromExternalId("name/0:7.0.0"));
        assertEquals("name/1:7.0.0", DataStripper.stripEpochFromExternalId("name/1:7.0.0"));
    }
    
}