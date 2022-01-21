package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.rpm;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.ComponentRelationshipPopulater;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;
import com.synopsys.integration.exception.IntegrationException;

public class RpmRelationshipPopulater implements ComponentRelationshipPopulater {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private static final List<String> RPM_DEPENDS_CMD = Arrays.asList("rpm","-qR");
    private static final Long CMD_TIMEOUT = 120000L;

    @Override
    public List<ComponentDetails> populateRelationshipInfo(final List<ComponentDetails> components, final @Nullable CmdExecutor cmdExecutor, DbRelationshipInfo dbRelationshipInfo) {
        for (ComponentDetails component : components) {
            List<String> dependsCmd = new LinkedList<>(RPM_DEPENDS_CMD);
            dependsCmd.add(component.getName());
            String[] dependencies = {};
            try {
                dependencies = cmdExecutor.executeCommand(dependsCmd, CMD_TIMEOUT);
            } catch (IntegrationException | UnsupportedEncodingException e) {
                logger.error(String.format("Running command %s resulted in an error.  Unable to populate relationship info for %s", String.join(" ", dependsCmd), component.getName()));
            }
            for (String dependency : dependencies) {
                components.stream()
                    .filter(componentDetails -> componentDetails.getName().equals(dependency))
                    .findFirst()
                    .ifPresent(dependentComponent -> component.getDependencies().add(dependentComponent));
                }
            }
        return components;
    }
}
