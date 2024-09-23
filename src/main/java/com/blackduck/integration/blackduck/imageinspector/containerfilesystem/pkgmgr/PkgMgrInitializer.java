/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr;

import java.io.File;
import java.io.IOException;

public interface PkgMgrInitializer {
    void initPkgMgrDir(File packageManagerDatabaseDir) throws IOException;
}
