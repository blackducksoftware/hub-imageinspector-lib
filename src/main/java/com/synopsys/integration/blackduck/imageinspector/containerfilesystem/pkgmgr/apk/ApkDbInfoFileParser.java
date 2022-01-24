package com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.apk;

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

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;

public class ApkDbInfoFileParser {

    public DbRelationshipInfo parseDbRelationshipInfoFromFile(File dbInfoFile) {
        Map<String, List<String>> compNamesToDependencies = new HashMap<>();
        Map<String, String> providedBinariesToCompNames = new HashMap<>();

        try {
            List<String> lines = Files.readAllLines(dbInfoFile.toPath()).stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
            String name = null;
            for (String line : lines) {
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
        } catch (IOException e) {
            // if reading file fails, return object with empty maps
        }
        return new DbRelationshipInfo(compNamesToDependencies, providedBinariesToCompNames);
    }
}