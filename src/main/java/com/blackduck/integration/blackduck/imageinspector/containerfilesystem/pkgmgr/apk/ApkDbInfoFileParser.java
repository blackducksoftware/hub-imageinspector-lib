/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.blackduck.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApkDbInfoFileParser {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DbRelationshipInfo parseDbRelationshipInfoFromFile(List<String> dbInfoFileLines) {
        Map<String, List<String>> compNamesToDependencies = new HashMap<>();
        Map<String, String> providedBinariesToCompNames = new HashMap<>();

        String name = null;
        dbInfoFileLines = dbInfoFileLines.stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
        for (String line : dbInfoFileLines) {
            char key = line.charAt(0);
            String value = line.substring(2).trim();
            if (key == 'P') {
                name = value;
            } else if (key == 'p') {
                for (String piece : value.split(" ")) {
                    providedBinariesToCompNames.put(piece.split("=")[0], name);
                }
            } else if (key == 'r' || key == 'D') {
                List<String> deps = Optional.ofNullable(compNamesToDependencies.get(name)).orElse(new LinkedList<>());
                deps.addAll(Arrays.stream(value.split(" "))
                    .map(dep -> dep.split(">=")[0])
                    .map(dep -> dep.split("<=")[0])
                    .map(dep -> dep.split("=")[0])
                    .collect(Collectors.toList()));
                compNamesToDependencies.put(name, deps);
            }
        }
        return new DbRelationshipInfo(compNamesToDependencies, providedBinariesToCompNames);
    }
}
