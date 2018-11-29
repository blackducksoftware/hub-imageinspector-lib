package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.BdioRelationship;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;

public class BdioGeneratorTest {

    @Test
    public void testFlat() {
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
        assertEquals(4, componentCount);
        assertEquals(0, componentChildCount);
    }

    @Test
    public void testHierarchical() {
        BdioGenerator bdioGenerator = new BdioGenerator();
        ImageComponentHierarchy imageComponentHierarchy = createImageComponentHierarchy();
        SimpleBdioDocument bdioDoc = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy("testCodeLocation", "testProject", "testProjectVersion", "ubuntu", imageComponentHierarchy, true, true);

        int layerCount = 0;
        int layerChildCount = 0;
        for (BdioComponent bdioComp : bdioDoc.components) {
            System.out.printf("Comp: %s/%s\n", bdioComp.name, bdioComp.version);
            layerCount++;
            for (BdioRelationship rel : bdioComp.relationships) {
                System.out.printf("\t%s: %s\n", rel.relationshipType, rel.related);
                layerChildCount++;
            }
        }
        assertEquals(6, layerCount);
        assertEquals(4, layerChildCount);
    }

    private ImageComponentHierarchy createImageComponentHierarchy() {
        ImageComponentHierarchy imageComponentHierarchy = new ImageComponentHierarchy("manifestFileContents", "imageConfigFileContents");
        final List<ComponentDetails> components1 = new ArrayList<>();
        components1.add(new ComponentDetails("comp1", "version1", "comp1ExternalId", "arch", "ubuntu"));
        components1.add(new ComponentDetails("comp2", "version2", "comp2ExternalId", "arch", "ubuntu"));
        LayerDetails layer1 = new LayerDetails("layer1", "layerMetadataFileContents", components1);
        imageComponentHierarchy.addLayer(layer1);

        final List<ComponentDetails> components2 = new ArrayList<>();
        components2.add(new ComponentDetails("comp1a", "version1a", "comp1aExternalId", "arch", "ubuntu"));
        components2.add(new ComponentDetails("comp2a", "version2a", "comp2aExternalId", "arch", "ubuntu"));
        LayerDetails layer2 = new LayerDetails("layer2", "layerMetadataFileContents", components2);
        imageComponentHierarchy.addLayer(layer2);

        final List<ComponentDetails> allComponents = new ArrayList<>();
        allComponents.addAll(components1);
        allComponents.addAll(components2);
        imageComponentHierarchy.setFinalComponents(allComponents);
        return imageComponentHierarchy;
    }
}
