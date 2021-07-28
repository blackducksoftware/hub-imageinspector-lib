package com.synopsys.integration.blackduck.imageinspector.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import com.synopsys.integration.blackduck.imageinspector.image.common.LayerDetails;
import org.junit.jupiter.api.Test;

public class LayerDetailsTest {

  @Test
  public void test() {
    LayerDetails layer = new LayerDetails(3, "sha:testLayer", Arrays.asList("layerCmd", "layerCmdArg"), new ArrayList<>());
    assertEquals("Layer03_sha_testLayer", layer.getLayerIndexedName());
  }

}
