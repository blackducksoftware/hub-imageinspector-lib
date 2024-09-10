package com.blackduck.integration.blackduck.imageinspector.image.oci;

import com.google.gson.Gson;
import com.synopsys.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OciLayoutParserTest {

    @Test
    void test() throws IntegrationException {
        OciLayoutParser parser = new OciLayoutParser(new Gson());
        assertEquals("1.0.0", parser.parseOciVersion("{\"imageLayoutVersion\": \"1.0.0\"}"));
    }
}
