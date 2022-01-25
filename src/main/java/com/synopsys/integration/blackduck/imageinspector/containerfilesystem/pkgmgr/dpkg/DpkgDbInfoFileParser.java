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

import com.synopsys.integration.blackduck.imageinspector.containerfilesystem.pkgmgr.pkgmgrdb.DbRelationshipInfo;

public class DpkgDbInfoFileParser {

    private static final String PACKAGE = "Package";
    private static final String PROVIDES = "Provides";
    private static final String DEPENDS = "Depends";
    private static final String PRE_DEPENDS = "Pre-Depends";

    public DbRelationshipInfo parseDbRelationshipInfoFromFile(File dbInfoFile) {
        Map<String, List<String>> compNamesToDependencies = new HashMap<>();
        Map<String, String> providedBinariesToCompNames = new HashMap<>();

        try {
            List<String> lines = Files.readAllLines(dbInfoFile.toPath()).stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
            String name = null;
            for (String line : lines) {
                String[] pieces = line.split(": ");
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
                    deps.addAll(Arrays.stream(value.split(","))
                        .map(dep -> dep.split("\\(>=")[0].trim())
                        .map(dep -> dep.split("\\(<=")[0].trim())
                        .map(dep -> dep.split("\\(=")[0].trim())
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