package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.BdioRelationship;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
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

    @Ignore
    @Test
    public void testFlatIncludeRemoved() {
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
        LayerDetails layer2 = new LayerDetails("layer2", "layerMetadataFileContents", components);
        imageComponentHierarchy.addLayer(layer2);
        return components;
    }

    private List<ComponentDetails> addLayer1(final ImageComponentHierarchy imageComponentHierarchy, final List<ComponentDetails> allComponents) {
        final List<ComponentDetails> components = new ArrayList<>();
        components.add(new ComponentDetails("comp0", "version0", "comp0ExternalId", "arch", "ubuntu"));
        components.add(new ComponentDetails("comp1", "version1", "comp1ExternalId", "arch", "ubuntu"));
        components.add(new ComponentDetails("comp2", "version2", "comp2ExternalId", "arch", "ubuntu"));
        allComponents.addAll(components);
        LayerDetails layer1 = new LayerDetails("layer1", "layerMetadataFileContents", components);
        imageComponentHierarchy.addLayer(layer1);
        return components;
    }
}
