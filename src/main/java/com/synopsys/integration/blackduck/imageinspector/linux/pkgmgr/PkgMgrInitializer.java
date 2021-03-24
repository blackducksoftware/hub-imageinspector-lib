/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.linux.pkgmgr;

import java.io.File;
import java.io.IOException;

public interface PkgMgrInitializer {
    void initPkgMgrDir(File packageManagerDatabaseDir) throws IOException;
}
