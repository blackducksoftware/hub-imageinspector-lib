package com.synopsys.integration.blackduck.imageinspector.linux;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.synopsys.integration.blackduck.imageinspector.linux.executor.Executor;
import com.synopsys.integration.exception.IntegrationException;

public class ExecutorTest {

    private static final String ARG = "\\{ epoch: \"%{E}\", name: \"%{N}\", version: \"%{V}-%{R}\", arch: \"%{ARCH}\" \\}\\n";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testUsingParts() throws UnsupportedEncodingException, IntegrationException {
        final List<String> cmdParts = new ArrayList<>();
        cmdParts.add("echo");
        cmdParts.add(ARG);
        System.out.printf("cmdParts via toString(): %s\n", cmdParts);
        final String[] results = Executor.executeCommand(cmdParts, 10000L);
        assertEquals(1, results.length);
        assertEquals(ARG, results[0]);
    }
}
