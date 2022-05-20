package com.synopsys.integration.blackduck.imageinspector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.model.BdioProject;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.bdio.model.dependency.ProjectDependency;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.imageinspector.bdio.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgrFactory;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.CommonRelationshipPopulater;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.exception.IntegrationException;

public class BdioGeneratorApiTest {
    private static final String[] pkgMgrListCmdOutputLines = {
        "ii  adduser                 3.116ubuntu1           all          add and remove users and groups",
        "ii  apt                     1.6.8                  amd64        commandline package manager"
    };

    @Test
    public void test() throws IntegrationException, IOException {
        PkgMgrFactory pkgMgrFactory = Mockito.mock(PkgMgrFactory.class);
        PkgMgr pkgMgr = Mockito.mock(PkgMgr.class);
        BdioGenerator bdioGenerator = Mockito.mock(BdioGenerator.class);
        BdioGeneratorApi api = new BdioGeneratorApi(new Gson(), pkgMgrFactory, bdioGenerator);

        final PackageManagerEnum pkgMgrType = PackageManagerEnum.DPKG;
        final String linuxDistroName = "ubuntu";

        final String blackDuckProjectName = "testProject";
        final String blackDuckProjectVersion = "testVersion";
        final String codeLocationName = "testCodelocationName";

        Mockito.when(pkgMgrFactory.createPkgMgr(Mockito.any(PackageManagerEnum.class), Mockito.anyString())).thenReturn(pkgMgr);
        List<ComponentDetails> comps = new ArrayList<>(2);
        comps.add(new ComponentDetails("adduser", "3.116ubuntu1", "adduser/3.116ubuntu1/all", "all", "ubuntu"));
        comps.add(new ComponentDetails("apt", "1.6.8", "apt/1.6.8/amd64", "amd64", "ubuntu"));
        Mockito.when(pkgMgr.extractComponentsFromPkgMgrOutput(null, linuxDistroName, pkgMgrListCmdOutputLines)).thenReturn(comps);

        DbRelationshipInfo dbRelationshipInfo = new DbRelationshipInfo(new HashMap<>(), new HashMap<>());
        Mockito.when(pkgMgr.createRelationshipPopulator(Mockito.any(CmdExecutor.class))).thenReturn(new CommonRelationshipPopulater(dbRelationshipInfo));

        SimpleBdioDocument bdioDoc = new SimpleBdioDocument();
        bdioDoc.setProject(new BdioProject());
        bdioDoc.getProject().name = "testBdioProject";

        ExternalId projectExternalId = bdioGenerator.createProjectExternalId(blackDuckProjectName, blackDuckProjectVersion, linuxDistroName);
        ProjectDependency projectDependency = bdioGenerator.createProjectDependency(blackDuckProjectName, blackDuckProjectVersion, projectExternalId);
        Mockito.when(bdioGenerator.generateFlatBdioDocumentFromComponents(projectDependency, codeLocationName, projectExternalId, comps)).thenReturn(bdioDoc);
        String[] mockedOutput = { "mockedOutput" };
        Mockito.when(bdioGenerator.getBdioAsStringArray(bdioDoc)).thenReturn(mockedOutput);
        String[] bdioLines = api.pkgListToBdio(pkgMgrType, linuxDistroName, pkgMgrListCmdOutputLines, blackDuckProjectName, blackDuckProjectVersion, codeLocationName);
        assertEquals(mockedOutput, bdioLines);
    }
}
