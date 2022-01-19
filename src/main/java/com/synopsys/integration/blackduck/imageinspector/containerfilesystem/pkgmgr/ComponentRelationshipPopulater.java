package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;

public interface ComponentRelationshipPopulater {
    List<ComponentDetails> populateRelationshipInfo(List<ComponentDetails> components, @Nullable CmdExecutor cmdExecutor);
}
