/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.dpkg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;

public class DpkgDbInfoFileParser {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String PACKAGE = "Package";
    private static final String PROVIDES = "Provides";
    private static final String DEPENDS = "Depends";
    private static final String PRE_DEPENDS = "Pre-Depends";

    public DbRelationshipInfo parseDbRelationshipInfoFromFile(List<String> dbInfoFileLines) {
        Map<String, List<String>> compNamesToDependencies = new HashMap<>();
        Map<String, String> providedBinariesToCompNames = new HashMap<>();

        String name = null;
        for (String line : dbInfoFileLines) {
            String[] pieces = line.split(": ");
            if (pieces.length < 2) {
                continue;
            }
            String key = pieces[0];
            String value = pieces[1].trim();
            if (key.equals(PACKAGE)) {
                name = value;
            } else if (key.equals(PROVIDES)) {
                for (String piece : value.split(",")) {
                    providedBinariesToCompNames.put(piece.split("=")[0].trim(), name);
                }
            } else if (key.equals(DEPENDS) || key.equals(PRE_DEPENDS)) {
                List<String> deps = Optional.ofNullable(compNamesToDependencies.get(name)).orElse(new LinkedList<>());
                for (String rawDep : value.split(",")) {
                    deps.addAll(Arrays.stream(rawDep.split("\\|"))
                        .map(dep -> dep.split("\\(>=")[0].trim())
                        .map(dep -> dep.split("\\(<=")[0].trim())
                        .map(dep -> dep.split("\\(=")[0].trim())
                        .map(dep -> dep.split("\\(<<")[0].trim())
                        .map(dep -> dep.split("\\(>>")[0].trim())
                        .collect(Collectors.toList()));
                    compNamesToDependencies.put(name, deps);
                }
            }
        }
        return new DbRelationshipInfo(compNamesToDependencies, providedBinariesToCompNames);
    }
}
