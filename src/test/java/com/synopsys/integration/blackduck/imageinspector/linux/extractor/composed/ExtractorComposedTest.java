package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.ApkExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.DpkgExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.RpmExecutor;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.model.BdioComponent;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

public class ExtractorComposedTest {

    @Test
    public void testApk() throws IntegrationException, IOException, InterruptedException {

        final String[] pkgMgrOutputLines = { "WARNING: Ignoring APKINDEX.adfa7ceb.tar.gz: No such file or directory",
                "alpine-baselayout-3.1.0-r0", "musl-utils-1.1.19-r10" };
        final PkgMgrExecutor pkgMgrExecutor = Mockito.mock(ApkExecutor.class);
        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(pkgMgrOutputLines);

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final ExtractorBehavior extractorBehavior = new ApkExtractorBehavior(pkgMgrExecutor);

        final File imagePkgMgrDir = new File("the code that uses this is mocked");
        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(imagePkgMgrDir, PackageManagerEnum.APK);
        final ExtractorComposed extractorComposed = new ExtractorComposed(simpleBdioFactory, extractorBehavior, imagePkgMgrDatabase);

        final SimpleBdioDocument bdio = extractorComposed.extract("dockerImageRepo", "dockerImageTag", "givenArch", "codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace");
        assertEquals(2, bdio.components.size());
        boolean foundComp1 = false;
        boolean foundComp2 = false;
        for (final BdioComponent comp : bdio.components) {
            System.out.printf("name: %s, version: %s, externalId: %s\n", comp.name, comp.version, comp.bdioExternalIdentifier.externalId);
            assertEquals("@preferredAliasNamespace", comp.bdioExternalIdentifier.forge);
            if ("alpine-baselayout".equals(comp.name)) {
                foundComp1 = true;
                assertEquals("alpine-baselayout/3.1.0-r0/givenArch", comp.bdioExternalIdentifier.externalId);
            }
            if ("musl-utils".equals(comp.name)) {
                foundComp2 = true;
                assertEquals("musl-utils/1.1.19-r10/givenArch", comp.bdioExternalIdentifier.externalId);
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

        final PkgMgrExecutor pkgMgrExecutor = Mockito.mock(DpkgExecutor.class);
        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(pkgMgrOutputLines);

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final ExtractorBehavior extractorBehavior = new DpkgExtractorBehavior(pkgMgrExecutor);

        final File imagePkgMgrDir = new File("the code that uses this is mocked");
        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(imagePkgMgrDir, PackageManagerEnum.DPKG);
        final ExtractorComposed extractorComposed = new ExtractorComposed(simpleBdioFactory, extractorBehavior, imagePkgMgrDatabase);

        final SimpleBdioDocument bdio = extractorComposed.extract("dockerImageRepo", "dockerImageTag", "architecture", "codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace");

        assertEquals(2, bdio.components.size());
        boolean foundComp1 = false;
        boolean foundComp2 = false;
        for (final BdioComponent comp : bdio.components) {
            System.out.printf("name: %s, version: %s, externalId: %s\n", comp.name, comp.version, comp.bdioExternalIdentifier.externalId);
            assertEquals("@preferredAliasNamespace", comp.bdioExternalIdentifier.forge);
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

        final String[] pkgMgrOutputLines = { "ncurses-base-5.9-14.20130511.el7_4.noarch",
                "krb5-libs-1.15.1-19.el7.x86_64" };

        final PkgMgrExecutor pkgMgrExecutor = Mockito.mock(RpmExecutor.class);
        Mockito.when(pkgMgrExecutor.runPackageManager(Mockito.any(ImagePkgMgrDatabase.class))).thenReturn(pkgMgrOutputLines);

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final ExtractorBehavior extractorBehavior = new RpmExtractorBehavior(pkgMgrExecutor);

        final File imagePkgMgrDir = new File("the code that uses this is mocked");
        final ImagePkgMgrDatabase imagePkgMgrDatabase = new ImagePkgMgrDatabase(imagePkgMgrDir, PackageManagerEnum.RPM);
        final ExtractorComposed extractorComposed = new ExtractorComposed(simpleBdioFactory, extractorBehavior, imagePkgMgrDatabase);

        final SimpleBdioDocument bdio = extractorComposed.extract("dockerImageRepo", "dockerImageTag", "architecture", "codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace");

        assertEquals(2, bdio.components.size());
        boolean foundComp1 = false;
        boolean foundComp2 = false;
        for (final BdioComponent comp : bdio.components) {
            System.out.printf("name: %s, version: %s, externalId: %s\n", comp.name, comp.version, comp.bdioExternalIdentifier.externalId);
            assertEquals("@preferredAliasNamespace", comp.bdioExternalIdentifier.forge);
            if ("ncurses-base".equals(comp.name)) {
                foundComp1 = true;
                assertEquals("ncurses-base/5.9-14.20130511.el7_4/noarch", comp.bdioExternalIdentifier.externalId);
            }
            if ("krb5-libs".equals(comp.name)) {
                foundComp2 = true;
                assertEquals("krb5-libs/1.15.1-19.el7/x86_64", comp.bdioExternalIdentifier.externalId);
            }
        }
        assertTrue(foundComp1);
        assertTrue(foundComp2);
    }

    @Test
    public void testNull() throws IntegrationException, IOException, InterruptedException {

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final ExtractorBehavior extractorBehavior = new NullExtractorBehavior();
        final ExtractorComposed extractorComposed = new ExtractorComposed(simpleBdioFactory, extractorBehavior, null);

        final SimpleBdioDocument bdio = extractorComposed.extract("dockerImageRepo", "dockerImageTag", "architecture", "codeLocationName", "projectName", "projectVersion", "preferredAliasNamespace");

        assertEquals(0, bdio.components.size());
    }
}
