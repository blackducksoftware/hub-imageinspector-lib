package com.synopsys.integration.blackduck.imageinspector.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;

public class ImageInspectionRequestBuilderTest {

    @Test
    public void testIncomplete() {
        final ImageInspectionRequestBuilder imageInspectionRequestBuilder = new ImageInspectionRequestBuilder();
        try {
            imageInspectionRequestBuilder
                .setDockerTarfilePath("x")
                .setOrganizeComponentsByLayer(false)
                .build();
            Assertions.fail("Expected exception due to incomplete request");
        } catch (IntegrationException e) {
            // Expected
        }
    }
}
