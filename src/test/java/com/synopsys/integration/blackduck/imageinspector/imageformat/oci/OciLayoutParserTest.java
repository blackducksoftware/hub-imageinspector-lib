package com.synopsys.integration.blackduck.imageinspector.imageformat.oci;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OciLayoutParserTest {

    @Test
    void test() throws IntegrationException {
        OciLayoutParser parser = new OciLayoutParser(new GsonBuilder());
        assertEquals("1.0.0", parser.parseOciVersion("{\"imageLayoutVersion\": \"1.0.0\"}"));
    }
}
