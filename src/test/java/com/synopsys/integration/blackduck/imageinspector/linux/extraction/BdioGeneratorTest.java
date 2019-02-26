package com.synopsys.integration.blackduck.imageinspector.linux.extraction;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.BdioRelationship;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.lib.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;

public class BdioGeneratorTest {

    @Test
    public void testFlatExcludeRemoved() {
        BdioGenerator bdioGenerator = new BdioGenerator();
        ImageComponentHierarchy imageComponentHierarchy = createImageComponentHierarchy();
        SimpleBdioDocument bdioDoc = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy("testCodeLocation", "testProject", "testProjectVersion", "ubuntu", imageComponentHierarchy, false, false);

        int componentCount = 0;
        int componentChildCount = 0;
        for (BdioComponent bdioComp : bdioDoc.components) {
            System.out.printf("Comp: %s/%s\n", bdioComp.name, bdioComp.version);
            componentCount++;
            for (BdioRelationship rel : bdioComp.relationships) {
                System.out.printf("\t%s: %s\n", rel.relationshipType, rel.related);
                componentChildCount++;
            }
        }
        assertEquals(3, componentCount);
        assertEquals(0, componentChildCount);
    }

    @Test
    public void testFlatIncludeRemoved() {
        BdioGenerator bdioGenerator = new BdioGenerator();
        ImageComponentHierarchy imageComponentHierarchy = createImageComponentHierarchy();
        SimpleBdioDocument bdioDoc = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy("testCodeLocation", "testProject", "testProjectVersion", "ubuntu", imageComponentHierarchy, false, true);

        int componentCount = 0;
        int componentChildCount = 0;
        for (BdioComponent bdioComp : bdioDoc.components) {
            System.out.printf("Comp: %s/%s\n", bdioComp.name, bdioComp.version);
            componentCount++;
            for (BdioRelationship rel : bdioComp.relationships) {
                System.out.printf("\t%s: %s\n", rel.relationshipType, rel.related);
                componentChildCount++;
            }
        }
        assertEquals(5, componentCount);
        assertEquals(0, componentChildCount);
    }

    @Test
    public void testHierarchicalIncludeRemoved() {
        BdioGenerator bdioGenerator = new BdioGenerator();
        ImageComponentHierarchy imageComponentHierarchy = createImageComponentHierarchy();
        SimpleBdioDocument bdioDoc = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy("testCodeLocation", "testProject", "testProjectVersion", "ubuntu", imageComponentHierarchy, true, true);

        int layerCount = 0;
        for (BdioRelationship rel : bdioDoc.project.relationships) {
            System.out.printf("Layer: %s: %s\n", rel.relationshipType, rel.related);
            layerCount++;
        }
        assertEquals(2, layerCount);
        System.out.printf("====\n");
        layerCount = 0;
        int compCount = 0;
        for (BdioComponent bdioComp : bdioDoc.components) {
            if (bdioComp.name.startsWith("Layer")) {
                System.out.printf("\t%s/%s\n", bdioComp.name, bdioComp.version);
                layerCount++;
            }
            for (BdioRelationship rel : bdioComp.relationships) {
                System.out.printf("\t\t%s: %s\n", rel.relationshipType, rel.related);
                compCount++;
            }
        }
        assertEquals(2, layerCount);
        assertEquals(6, compCount);
    }

    @Test
    public void testHierarchicalExcludeRemoved() {
        BdioGenerator bdioGenerator = new BdioGenerator();
        ImageComponentHierarchy imageComponentHierarchy = createImageComponentHierarchy();
        SimpleBdioDocument bdioDoc = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy("testCodeLocation", "testProject", "testProjectVersion", "ubuntu", imageComponentHierarchy, true, false);

        int layerCount = 0;
        for (BdioRelationship rel : bdioDoc.project.relationships) {
            System.out.printf("Layer: %s: %s\n", rel.relationshipType, rel.related);
            layerCount++;
        }
        assertEquals(2, layerCount);
        System.out.printf("====\n");
        int compCount = 0;
        for (BdioComponent bdioComp : bdioDoc.components) {
            if (bdioComp.name.startsWith("layer")) {
                System.out.printf("\t%s/%s\n", bdioComp.name, bdioComp.version);
            }
            for (BdioRelationship rel : bdioComp.relationships) {
                System.out.printf("\t\t%s: %s\n", rel.relationshipType, rel.related);
                compCount++;
            }
        }
        assertEquals(4, compCount);
    }


    @Test
    public void testEmpty() {

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();

        final BdioGenerator bdioGenerator = new BdioGenerator(simpleBdioFactory);
        List<ComponentDetails>  comps = new ArrayList<>(0);
        final SimpleBdioDocument bdio = bdioGenerator.generateFlatBdioDocumentFromComponents("codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace", comps);

        assertEquals(0, bdio.components.size());
    }

    private ImageComponentHierarchy createImageComponentHierarchy() {
        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy("manifestFileContents", "imageConfigFileContents");
        final List<ComponentDetails> allComponents = new ArrayList<>();
        addLayer1(imageComponentHierarchy, allComponents);
        List<ComponentDetails> layer2Components = addLayer2(imageComponentHierarchy, allComponents);

        imageComponentHierarchy.setFinalComponents(layer2Components);
        return imageComponentHierarchy;
    }

    private List<ComponentDetails> addLayer2(final ImageComponentHierarchy imageComponentHierarchy, final List<ComponentDetails> allComponents) {
        final List<ComponentDetails> components = new ArrayList<>();
        components.add(new ComponentDetails("comp0", "version0", "comp0ExternalId", "arch", "ubuntu"));
        components.add(new ComponentDetails("comp1a", "version1a", "comp1aExternalId", "arch", "ubuntu"));
        components.add(new ComponentDetails("comp2a", "version2a", "comp2aExternalId", "arch", "ubuntu"));
        allComponents.addAll(components);
        LayerDetails layer2 = new LayerDetails(1, "sha:layer2","layerMetadataFileContents", Arrays.asList("layerCmd", "layerCmdArg"), components);
        imageComponentHierarchy.addLayer(layer2);
        return components;
    }

    private List<ComponentDetails> addLayer1(final ImageComponentHierarchy imageComponentHierarchy, final List<ComponentDetails> allComponents) {
        final List<ComponentDetails> components = new ArrayList<>();
        components.add(new ComponentDetails("comp0", "version0", "comp0ExternalId", "arch", "ubuntu"));
        components.add(new ComponentDetails("comp1", "version1", "comp1ExternalId", "arch", "ubuntu"));
        components.add(new ComponentDetails("comp2", "version2", "comp2ExternalId", "arch", "ubuntu"));
        allComponents.addAll(components);
        LayerDetails layer1 = new LayerDetails(0, "sha:layer1","layerMetadataFileContents", Arrays.asList("layerCmd", "layerCmdArg"), components);
        imageComponentHierarchy.addLayer(layer1);
        return components;
    }
}
