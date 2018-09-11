package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.synopsys.integration.hub.bdio.BdioWriter;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;

public class TempTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws IOException {
        final Forge forge = new Forge("/", "/", "@fedora");
        final ExternalId extId = new SimpleBdioFactory().createArchitectureExternalId(forge, "libcap", "2.16-5.5.el6", "x86_64");
        System.out.printf("extId: %s\n", extId);
        final Dependency dep = new SimpleBdioFactory().createDependency("libcap", "2.16-5.5.el6", extId);
        System.out.printf("adding %s as child to dependency node tree; dataId: %s\n", dep.name, dep.externalId.createBdioId());

        final ExternalId projectExternalId = new SimpleBdioFactory().createNameVersionExternalId(forge, "SB001", "unitTest");
        final SimpleBdioDocument bdioDocument = new SimpleBdioFactory().createSimpleBdioDocument("unitTestCodeLocation", "SB001", "unitTest", projectExternalId);
        final MutableDependencyGraph dependencies = new SimpleBdioFactory().createMutableDependencyGraph();
        dependencies.addChildToRoot(dep);
        new SimpleBdioFactory().populateComponents(bdioDocument, projectExternalId, dependencies);

        final File outputBdioFile = new File(new File("/tmp"), "testBdioFile.jsonld");
        final FileOutputStream outputBdioStream = new FileOutputStream(outputBdioFile);
        System.out.printf("Writing BDIO to %s\n", outputBdioFile.getAbsolutePath());
        try (BdioWriter bdioWriter = new BdioWriter(new Gson(), outputBdioStream)) {
            bdioWriter.writeSimpleBdioDocument(bdioDocument);
        }
    }

}
