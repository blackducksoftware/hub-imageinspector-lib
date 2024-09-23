package com.blackduck.integration.blackduck.imageinspector.containerfilesystem;

import com.blackduck.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.blackduck.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.PkgMgr;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PkgMgrDbExtractorTest {

    @Test
    void testNoOverride() throws PkgMgrDataNotFoundException {
        File fullFileSystem = Mockito.mock(File.class);
        List<PkgMgr> pkgMgrs = new ArrayList<>();
        PkgMgr pkgMgr = Mockito.mock(PkgMgr.class);
        Mockito.when(pkgMgr.isApplicable(fullFileSystem)).thenReturn(true);
        Mockito.when(pkgMgr.getType()).thenReturn(PackageManagerEnum.DPKG);
        File pkgMgrDir = Mockito.mock(File.class);
        Mockito.when(pkgMgr.getImagePackageManagerDirectory(fullFileSystem)).thenReturn(pkgMgrDir);
        pkgMgrs.add(pkgMgr);
        LinuxDistroExtractor linuxDistroExtractor = Mockito.mock(LinuxDistroExtractor.class);
        Mockito.when(linuxDistroExtractor.extract(fullFileSystem)).thenReturn(Optional.of("ubuntu"));
        PkgMgrDbExtractor pkgMgrDbExtractor = new PkgMgrDbExtractor(pkgMgrs, linuxDistroExtractor);

        ContainerFileSystem containerFileSystem = Mockito.mock(ContainerFileSystem.class);
        Mockito.when(containerFileSystem.getTargetImageFileSystemFull()).thenReturn(fullFileSystem);
        String targetLinuxDistroOverride = null;

        ContainerFileSystemWithPkgMgrDb result = pkgMgrDbExtractor.extract(containerFileSystem, targetLinuxDistroOverride);

        assertEquals("ubuntu", result.getLinuxDistroName());
        assertEquals(pkgMgr, result.getPkgMgr());
        assertEquals(containerFileSystem, result.getContainerFileSystem());
    }
}
