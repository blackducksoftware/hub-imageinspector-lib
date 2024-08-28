/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr;

import java.util.List;

import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.components.ComponentDetails;

public interface ComponentRelationshipPopulater {
    List<ComponentDetails> populateRelationshipInfo(List<ComponentDetails> components);
}
