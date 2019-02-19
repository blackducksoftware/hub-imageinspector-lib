package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.model.BdioComponent;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.apk.ApkPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.dpkg.DpkgPkgMgr;
import com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr.rpm.RpmPkgMgr;
import com.synopsys.integration.exception.IntegrationException;

public class ExtractorComposedTest {

    // TODO think about test class name and package

    @Test
    public void testApk() throws IntegrationException, IOException, InterruptedException {

        final String[] pkgMgrOutputLines = { "WARNING: Ignoring APKINDEX.adfa7ceb.tar.gz: No such file or directory",
                "alpine-baselayout-3.1.0-r0", "musl-utils-1.1.19-r10" };

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final PkgMgr pkgMgr = new ApkPkgMgr();
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
        final PkgMgr pkgMgr = new DpkgPkgMgr();
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
            if ("111:krb5-libs".equals(comp.name)) {
                foundComp2 = true;
                assertEquals("111:krb5-libs/1.15.1-19.el7/x86_64", comp.bdioExternalIdentifier.externalId);
                assertEquals("1.15.1-19.el7", comp.version);
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

    // TODO this is only testing BdioGenerator, not pkg mgrs, so does it really belong here?
    @Test
    public void testNull() throws IntegrationException, IOException, InterruptedException {

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();

        final BdioGenerator bdioGenerator = new BdioGenerator(simpleBdioFactory);
        List<ComponentDetails>  comps = new ArrayList<>(0);
        final SimpleBdioDocument bdio = bdioGenerator.generateFlatBdioDocumentFromComponents("codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace", comps);

        assertEquals(0, bdio.components.size());
    }

    private SimpleBdioDocument getBdioDocumentForRpmPackages(final String[] pkgMgrOutputLines) throws IntegrationException, IOException, InterruptedException {
        final PkgMgr pkgMgr = new RpmPkgMgr(new Gson());
        final File imageFilesystem = new File("src/test/resources/testRpmFileSystem");
        assertTrue(pkgMgr.isApplicable(imageFilesystem));
        assertEquals("rpm", pkgMgr.getImagePackageManagerDirectory(imageFilesystem).getName());
        List<ComponentDetails> comps = pkgMgr.extractComponentsFromPkgMgrOutput(imageFilesystem, "centos", pkgMgrOutputLines);
        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final BdioGenerator bdioGenerator = new BdioGenerator(simpleBdioFactory);
        final SimpleBdioDocument bdio = bdioGenerator.generateFlatBdioDocumentFromComponents("codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace", comps);
        return bdio;
    }
}
