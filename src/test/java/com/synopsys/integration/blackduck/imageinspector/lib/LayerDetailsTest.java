package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class LayerDetailsTest {

  @Test
  public void test() {
    LayerDetails layer = new LayerDetails(3, "testLayerDotTarDirName", "test metadata file contents", new ArrayList<>());
    assertEquals("Layer03_testLayerDotTarDirName", layer.getLayerIndexedName());
  }

}
