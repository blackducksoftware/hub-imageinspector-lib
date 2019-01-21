package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class LayerDetailsTest {

  @Test
  public void test() {
    LayerDetails layer = new LayerDetails(3, "sha:testLayer", "test metadata file contents", new ArrayList<>());
    assertEquals("Layer03_sha_testLayer", layer.getLayerIndexedName());
  }

}
