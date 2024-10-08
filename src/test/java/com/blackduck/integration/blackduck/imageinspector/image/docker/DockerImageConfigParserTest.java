package com.blackduck.integration.blackduck.imageinspector.image.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.blackduck.imageinspector.image.common.CommonImageConfigParser;

public class DockerImageConfigParserTest {

  private final static String CONFIG_FILE_CONTENTS = "{\"architecture\":\"amd64\",\"config\":{\"Hostname\":\"\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"],\"Cmd\":[\"/bin/sh\"],\"ArgsEscaped\":true,\"Image\":\"sha256:ce244ca5cf823254a1dff4ea35589dcdbe540266820f401a86b7b8dc9eda8f19\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":null},\"container\":\"35bf94cc91dd11f6bd36502cefc82fd4515b20e0181b49e7c316bd78ff7c75d6\",\"container_config\":{\"Hostname\":\"35bf94cc91dd\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"],\"Cmd\":[\"/bin/sh\",\"-c\",\"#(nop) \",\"CMD [\\\"/bin/sh\\\"]\"],\"ArgsEscaped\":true,\"Image\":\"sha256:ce244ca5cf823254a1dff4ea35589dcdbe540266820f401a86b7b8dc9eda8f19\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":{}},\"created\":\"2019-01-30T22:19:52.734509838Z\",\"docker_version\":\"18.06.1-ce\",\"history\":[{\"created\":\"2019-01-30T22:19:52.585366638Z\",\"created_by\":\"/bin/sh -c #(nop) ADD file:2a1fc9351afe35698918545b2d466d9805c2e8afcec52f916785ee65bbafeced in / \"},{\"created\":\"2019-01-30T22:19:52.734509838Z\",\"created_by\":\"/bin/sh -c #(nop)  CMD [\\\"/bin/sh\\\"]\",\"empty_layer\":true}],\"os\":\"linux\",\"rootfs\":{\"type\":\"layers\",\"diff_ids\":[\"sha256:503e53e365f34399c4d58d8f4e23c161106cfbce4400e3d0a0357967bad69390\"]}}";

  @Test
  public void testImageConfigParser() {
    CommonImageConfigParser parser = new CommonImageConfigParser(new Gson());
    List<String> layerIds = parser.parseExternalLayerIds(CONFIG_FILE_CONTENTS);
    assertEquals(1, layerIds.size());
    assertEquals("sha256:503e53e365f34399c4d58d8f4e23c161106cfbce4400e3d0a0357967bad69390", layerIds.get(0));
  }

}
