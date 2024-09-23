package com.blackduck.integration.blackduck.imageinspector.linux;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.exception.IntegrationException;

public class CmdExecutorTest {

    private static final String ARG = "\\{ epoch: \"%{E}\", name: \"%{N}\", version: \"%{V}-%{R}\", arch: \"%{ARCH}\" \\}\\n";

    @BeforeAll
    public static void setUpBeforeAll() throws Exception {
    }

    @AfterAll
    public static void tearDownAfterAll() throws Exception {
    }

    @Test
    public void testUsingParts() throws UnsupportedEncodingException, IntegrationException {
        final List<String> cmdParts = new ArrayList<>();
        cmdParts.add("echo");
        cmdParts.add(ARG);
        System.out.printf("cmdParts via toString(): %s\n", cmdParts);
        final String[] results = (new CmdExecutor()).executeCommand(cmdParts, 10000L);
        assertEquals(1, results.length);
        assertEquals(ARG, results[0]);
    }
}
