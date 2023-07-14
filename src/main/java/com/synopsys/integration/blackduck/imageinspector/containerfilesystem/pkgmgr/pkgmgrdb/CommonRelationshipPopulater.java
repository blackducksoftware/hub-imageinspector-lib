/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.ComponentRelationshipPopulater;
import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import com.synopsys.integration.blackduck.imageinspector.linux.CmdExecutor;

public class CommonRelationshipPopulater implements ComponentRelationshipPopulater {
    private DbRelationshipInfo dbRelationshipInfo;

    public CommonRelationshipPopulater(final DbRelationshipInfo dbRelationshipInfo) {
        this.dbRelationshipInfo = dbRelationshipInfo;
    }

    @Override
    public List<ComponentDetails> populateRelationshipInfo(final List<ComponentDetails> components) {
        Map<String,ComponentDetails> componentNameVersionsToDetails = mapComponentNameVersionToDetails(components);
        Map<String, List<String>> compNamesToDependencies = dbRelationshipInfo.getCompNamesToDependencies();
        Map<String, String> providedBinariesToCompNames = dbRelationshipInfo.getProvidedBinariesToCompNames();

        for (Map.Entry<String, List<String>> depEntry : compNamesToDependencies.entrySet()) {
            ComponentDetails component = componentNameVersionsToDetails.get(depEntry.getKey());
            if (component != null) {
                for (String dependency : depEntry.getValue()) {
                    ComponentDetails componentDependency = componentNameVersionsToDetails.get(dependency);
                    if (componentDependency != null) {
                        addDependency(component, componentDependency);
                    }
                    ComponentDetails componentProvidingBinaryDependency = componentNameVersionsToDetails.get(providedBinariesToCompNames.get(dependency));
                    if (componentProvidingBinaryDependency != null) {
                        addDependency(component, componentProvidingBinaryDependency);
                    }
                }
            }
        }
        return components;
    }

    // this implementation assumes dependency declarations will not refer to multiple versions of the same component
    private Map<String,ComponentDetails> mapComponentNameVersionToDetails(List<ComponentDetails> components) {
        Map<String,ComponentDetails> map = new HashMap<>();
        components.forEach(component -> map.put(component.getName(), component));
        return map;
    }

    private void addDependency(ComponentDetails component, ComponentDetails dependency) {
        List<ComponentDetails> dependencies = Optional.ofNullable(component.getDependencies()).orElse(new LinkedList<>());
        dependencies.add(dependency);
        component.setDependencies(dependencies);
    }
}
