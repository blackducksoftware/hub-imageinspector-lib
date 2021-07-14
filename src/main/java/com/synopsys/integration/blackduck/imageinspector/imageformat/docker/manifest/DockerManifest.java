/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.synopsys.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import com.synopsys.integration.blackduck.imageinspector.lib.ManifestLayerMapping;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.Stringable;

public class DockerManifest extends Stringable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final File tarExtractionDirectory;

    public DockerManifest(final File tarExtractionDirectory) {
        this.tarExtractionDirectory = tarExtractionDirectory;
    }

    public ManifestLayerMapping getLayerMapping(final String targetImageName, final String targetTagName) throws IntegrationException, IOException {
        logger.debug(String.format("getLayerMappings(): targetImageName: %s; targetTagName: %s", targetImageName, targetTagName));
        final List<DockerImageInfo> images = getManifestContents();
        logger.debug(String.format("getLayerMappings(): images.size(): %d", images.size()));
        validateImageSpecificity(images, targetImageName, targetTagName);
        for (final DockerImageInfo image : images) {
            logger.trace(String.format("getLayerMappings(): image: %s", image));
            final String foundRepoTag = findRepoTag(images.size(), image, targetImageName, targetTagName);
            if (foundRepoTag == null) {
                continue;
            }
            logger.debug(String.format("foundRepoTag: %s", foundRepoTag));
            final ImageNameResolver resolver = new ImageNameResolver(foundRepoTag);
            logger.debug(String.format("translated repoTag to: repo: %s, tag: %s", resolver.getNewImageRepo().get(), resolver.getNewImageTag().get()));
            return createMapping(image, resolver.getNewImageRepo().get(), resolver.getNewImageTag().get());
        }
        throw new IntegrationException(String.format("Layer mapping for repo:tag %s:%s not found in manifest.json", targetImageName, targetTagName));
    }

    private String findRepoTag(final int numImages, final DockerImageInfo image, final String targetImageName, final String targetTagName) {
        // user didn't specify which image, and there is only one: return it
        if (numImages == 1 && StringUtils.isBlank(targetImageName) && StringUtils.isBlank(targetTagName)) {
            logger.debug(String.format("User did not specify a repo:tag, and there's only one image; inspecting that one: %s", getRepoTag(image)));
            return getRepoTag(image);
        }
        final String targetRepoTag = deriveSpecifiedRepoTag(targetImageName, targetTagName);
        logger.debug(String.format("findRepoTag(): specifiedRepoTag: %s", targetRepoTag));
        for (final String repoTag : image.repoTags) {
            logger.trace(String.format("Target repo tag %s; checking %s", targetRepoTag, repoTag));
            if (StringUtils.compare(repoTag, targetRepoTag) == 0) {
                logger.trace(String.format("Found the targetRepoTag %s", targetRepoTag));
                return repoTag;
            }
            if (targetRepoTag.endsWith("/" + repoTag)) {
                logger.trace(String.format("Matched the targetRepoTag %s to %s by ignoring the repository prefix", targetRepoTag, repoTag));
                return repoTag;
            }
        }
        return null;
    }

    private String getRepoTag(final DockerImageInfo image) {
        if (image.repoTags == null || image.repoTags.isEmpty()) {
            return "null:null";
        }
        return image.repoTags.get(0);
    }

    private ManifestLayerMapping createMapping(final DockerImageInfo image, final String imageName, final String tagName) {
        final List<String> layerIds = new ArrayList<>();
        for (final String layer : image.layers) {
            layerIds.add(layer.substring(0, layer.indexOf('/')));
        }
        final ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, image.config, layerIds);
        logger.trace(String.format("Found layer mapping: Image %s, Tag %s, Layers: %s", mapping.getImageName(), mapping.getTagName(), mapping.getLayerInternalIds()));
        return mapping;
    }

    private String deriveSpecifiedRepoTag(final String dockerImageName, final String dockerTagName) {
        String specifiedRepoTag = "";
        if (StringUtils.isNotBlank(dockerImageName)) {
            specifiedRepoTag = String.format("%s:%s", dockerImageName, dockerTagName);
        }
        return specifiedRepoTag;
    }

    private void validateImageSpecificity(final List<DockerImageInfo> images, final String targetImageName, final String targetTagName) throws IntegrationException {
        if (images.size() > 1 && (StringUtils.isBlank(targetImageName) || StringUtils.isBlank(targetTagName))) {
            final String msg = "When the manifest contains multiple images or tags, the target image and tag to inspect must be specified";
            logger.debug(msg);
            throw new IntegrationException(msg);
        }
    }

    private List<DockerImageInfo> getManifestContents() throws IOException {
        logger.trace("getManifestContents()");
        final List<DockerImageInfo> images = new ArrayList<>();
        logger.debug("getManifestContents(): extracting manifest file content");
        final String manifestContentString = extractManifestFileContent();
        logger.trace(String.format("getManifestContents(): parsing: %s", manifestContentString));
        final JsonParser parser = new JsonParser();
        final JsonArray manifestContent = parser.parse(manifestContentString).getAsJsonArray();
        final Gson gson = new Gson();
        for (final JsonElement element : manifestContent) {
            logger.trace(String.format("getManifestContents(): element: %s", element.toString()));
            images.add(gson.fromJson(element, DockerImageInfo.class));
        }
        return images;
    }

    private String extractManifestFileContent() throws IOException {
        final File manifest = new File(tarExtractionDirectory, "manifest.json");
        return StringUtils.join(FileUtils.readLines(manifest, StandardCharsets.UTF_8), "\n");
    }
}
