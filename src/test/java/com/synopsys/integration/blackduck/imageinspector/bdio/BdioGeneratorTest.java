package com.synopsys.integration.blackduck.imageinspector.bdio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.BdioRelationship;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.lib.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm.RpmPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

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

    @Test
    public void testApk() throws IntegrationException, IOException, InterruptedException {

        final String[] pkgMgrOutputLines = { "WARNING: Ignoring APKINDEX.adfa7ceb.tar.gz: No such file or directory",
            "alpine-baselayout-3.1.0-r0", "musl-utils-1.1.19-r10" };

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final PkgMgr pkgMgr = new ApkPkgMgr(new FileOperations());
        final File imageFilesystem = new File("src/test/resources/testApkFileSystem");
        assertTrue(pkgMgr.isApplicable(imageFilesystem));
        assertEquals("apk", pkgMgr.getImagePackageManagerDirectory(imageFilesystem).getName());
        List<ComponentDetails> comps = pkgMgr.extractComponentsFromPkgMgrOutput(imageFilesystem, "alpine", pkgMgrOutputLines);
        final BdioGenerator bdioGenerator = new BdioGenerator(simpleBdioFactory);
        final SimpleBdioDocument bdio = bdioGenerator.generateFlatBdioDocumentFromComponents("codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace", comps);
        assertEquals(2, bdio.components.size());
        boolean foundComp1 = false;
        boolean foundComp2 = false;
        for (final BdioComponent bdioComp : bdio.components) {
            System.out.printf("name: %s, version: %s, externalId: %s\n", bdioComp.name, bdioComp.version, bdioComp.bdioExternalIdentifier.externalId);
            assertEquals("@alpine", bdioComp.bdioExternalIdentifier.forge);
            if ("alpine-baselayout".equals(bdioComp.name)) {
                foundComp1 = true;
                assertEquals("alpine-baselayout/3.1.0-r0/x86_64", bdioComp.bdioExternalIdentifier.externalId);
            }
            if ("musl-utils".equals(bdioComp.name)) {
                foundComp2 = true;
                assertEquals("musl-utils/1.1.19-r10/x86_64", bdioComp.bdioExternalIdentifier.externalId);
            }
        }
        assertTrue(foundComp1);
        assertTrue(foundComp2);
    }

    @Test
    public void testDpkg() throws IntegrationException, IOException, InterruptedException {

        final String[] pkgMgrOutputLines = { "+++-=======================-======================-============-========================================================================",
            "ii  libstdc++6:amd64        8-20180414-1ubuntu2    amd64        GNU Standard C++ Library v3",
            "ii  login                   1:4.5-1ubuntu1         amd64        system login tools" };

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final PkgMgr pkgMgr = new DpkgPkgMgr(new FileOperations());
        final File imageFilesystem = new File("src/test/resources/testDpkgFileSystem");
        assertTrue(pkgMgr.isApplicable(imageFilesystem));
        assertEquals("dpkg", pkgMgr.getImagePackageManagerDirectory(imageFilesystem).getName());
        List<ComponentDetails> comps = pkgMgr.extractComponentsFromPkgMgrOutput(imageFilesystem, "ubuntu", pkgMgrOutputLines);
        final BdioGenerator bdioGenerator = new BdioGenerator(simpleBdioFactory);
        final SimpleBdioDocument bdio = bdioGenerator.generateFlatBdioDocumentFromComponents("codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace", comps);

        assertEquals(2, bdio.components.size());
        boolean foundComp1 = false;
        boolean foundComp2 = false;
        for (final BdioComponent comp : bdio.components) {
            System.out.printf("name: %s, version: %s, externalId: %s\n", comp.name, comp.version, comp.bdioExternalIdentifier.externalId);
            assertEquals("@ubuntu", comp.bdioExternalIdentifier.forge);
            if ("libstdc++6".equals(comp.name)) {
                foundComp1 = true;
                assertEquals("libstdc++6/8-20180414-1ubuntu2/amd64", comp.bdioExternalIdentifier.externalId);
            }
            if ("login".equals(comp.name)) {
                foundComp2 = true;
                assertEquals("login/1:4.5-1ubuntu1/amd64", comp.bdioExternalIdentifier.externalId);
            }
        }
        assertTrue(foundComp1);
        assertTrue(foundComp2);
    }

    @Test
    public void testRpm() throws IntegrationException, IOException, InterruptedException {
        final String[] pkgMgrOutputLines = {
            "{ epoch: \"(none)\", name: \"ncurses-base\", version: \"5.9-14.20130511.el7_4\", arch: \"noarch\" }",
            "{ epoch: \"111\", name: \"krb5-libs\", version: \"1.15.1-19.el7\", arch: \"x86_64\" }"
        };
        final SimpleBdioDocument bdio = getBdioDocumentForRpmPackages(pkgMgrOutputLines);

        assertEquals(2, bdio.components.size());
        boolean foundComp1 = false;
        boolean foundComp2 = false;
        for (final BdioComponent comp : bdio.components) {
            System.out.printf("name: %s, version: %s, externalId: %s\n", comp.name, comp.version, comp.bdioExternalIdentifier.externalId);
            assertEquals("@centos", comp.bdioExternalIdentifier.forge);
            if ("ncurses-base".equals(comp.name)) {
                foundComp1 = true;
                assertEquals("ncurses-base/5.9-14.20130511.el7_4/noarch", comp.bdioExternalIdentifier.externalId);
                assertEquals("5.9-14.20130511.el7_4", comp.version);
            }
            if ("krb5-libs".equals(comp.name)) {
                foundComp2 = true;
                assertEquals("krb5-libs/111:1.15.1-19.el7/x86_64", comp.bdioExternalIdentifier.externalId);
                assertEquals("111:1.15.1-19.el7", comp.version);
            }
        }
        assertTrue(foundComp1);
        assertTrue(foundComp2);
    }

    @Test
    public void testRpmFromCentosMinusVimImage() throws IntegrationException, IOException, InterruptedException {
        final List<String> lines = FileUtils.readLines(new File("src/test/resources/pkgMgrOutput/rpm/centos_minus_vim_plus_bacula.txt"), StandardCharsets.UTF_8);
        final String[] pkgMgrOutputLines = lines.toArray(new String[0]);
        final SimpleBdioDocument bdio = getBdioDocumentForRpmPackages(pkgMgrOutputLines);
        final String comp1Name = "cracklib";
        final String comp1Version = "2.9.0-11.el7";
        final String comp1Arch = "x86_64";
        boolean foundComp1 = false;
        boolean foundComp2 = false;
        for (final BdioComponent comp : bdio.components) {
            System.out.printf("name: %s, version: %s, externalId: %s\n", comp.name, comp.version, comp.bdioExternalIdentifier.externalId);
            assertEquals("@centos", comp.bdioExternalIdentifier.forge);
            if (comp1Name.equals(comp.name)) {
                foundComp1 = true;
                assertEquals(String.format("%s/%s/%s", comp1Name, comp1Version, comp1Arch), comp.bdioExternalIdentifier.externalId);
            }
            if ("cracklib-dicts".equals(comp.name)) {
                foundComp2 = true;
                assertEquals("cracklib-dicts/2.9.0-11.el7/x86_64", comp.bdioExternalIdentifier.externalId);
            }
        }
        assertTrue(String.format("component %s/%s/%s not found", comp1Name, comp1Version, comp1Arch), foundComp1);
        assertTrue(foundComp2);
        assertEquals(189, bdio.components.size());
    }

    private SimpleBdioDocument getBdioDocumentForRpmPackages(final String[] pkgMgrOutputLines) throws IntegrationException, IOException, InterruptedException {
        final PkgMgr pkgMgr = new RpmPkgMgr(new Gson(), new FileOperations());
        final File imageFilesystem = new File("src/test/resources/testRpmFileSystem");
        assertTrue(pkgMgr.isApplicable(imageFilesystem));
        assertEquals("rpm", pkgMgr.getImagePackageManagerDirectory(imageFilesystem).getName());
        List<ComponentDetails> comps = pkgMgr.extractComponentsFromPkgMgrOutput(imageFilesystem, "centos", pkgMgrOutputLines);
        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final BdioGenerator bdioGenerator = new BdioGenerator(simpleBdioFactory);
        final SimpleBdioDocument bdio = bdioGenerator.generateFlatBdioDocumentFromComponents("codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace", comps);
        return bdio;
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
