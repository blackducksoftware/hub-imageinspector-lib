/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgr;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioWriter;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;

public abstract class Extractor {
    private final Logger logger = LoggerFactory.getLogger(Extractor.class);
    private PackageManagerEnum packageManagerEnum;
    private PkgMgrExecutor executor;
    private List<Forge> defaultForges;

    public abstract void init();

    public abstract String deriveArchitecture(final File targetImageFileSystemRootDir) throws IOException;

    public abstract void extractComponents(MutableDependencyGraph dependencies, String dockerImageRepo, String dockerImageTag, String architecture, String[] packageList, final String preferredAliasNamespace);

    public final PackageManagerEnum getPackageManagerEnum() {
        return packageManagerEnum;
    }

    public final SimpleBdioDocument extract(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgr imagePkgMgr, final String architecture, final String codeLocationName, final String projectName,
            final String projectVersion,
            final String preferredAliasNamespace)
            throws IntegrationException, IOException, InterruptedException {

        final SimpleBdioDocument bdioDocument = extractBdio(dockerImageRepo, dockerImageTag, imagePkgMgr, architecture, codeLocationName, projectName, projectVersion, preferredAliasNamespace);
        return bdioDocument;
    }

    public final SimpleBdioDocument createEmptyBdio(final String codeLocationName, final String projectName, final String version)
            throws IntegrationException, IOException, InterruptedException {
        final ExternalId projectExternalId = new SimpleBdioFactory().createNameVersionExternalId(new Forge("/", "/", "unknown"), projectName, version);
        final SimpleBdioDocument bdioDocument = new SimpleBdioFactory().createSimpleBdioDocument(codeLocationName, projectName, version, projectExternalId);
        return bdioDocument;
    }

    public static final void writeBdio(final BdioWriter bdioWriter, final SimpleBdioDocument bdioDocument) {
        new SimpleBdioFactory().writeSimpleBdioDocument(bdioWriter, bdioDocument);
    }

    protected final void initValues(final PackageManagerEnum packageManagerEnum, final PkgMgrExecutor executor, final List<Forge> defaultForges) {
        this.packageManagerEnum = packageManagerEnum;
        this.executor = executor;
        this.defaultForges = defaultForges;
    }

    protected void createBdioComponent(final MutableDependencyGraph dependencies, final String name, final String version, final String externalId, final String arch, final String preferredAliasNamespace) {
        if (preferredAliasNamespace != null) {
            final String forgeId = String.format("@%s", preferredAliasNamespace);
            logger.debug(String.format("Generating component with preferred alias namespace (forge=@LinuxDistro): %s", forgeId));
            final Forge preferredNamespaceForge = new Forge("/", "/", forgeId);
            addDependency(dependencies, name, version, arch, preferredNamespaceForge);
        } else {
            logger.debug("Generating components with all package manager-appropriate namespaces (forges)");
            for (final Forge forge : defaultForges) {
                addDependency(dependencies, name, version, arch, forge);
            }
        }
    }

    private SimpleBdioDocument extractBdio(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgr imagePkgMgr, final String architecture, final String codeLocationName, final String projectName, final String version,
            final String preferredAliasNamespace)
            throws IntegrationException, IOException, InterruptedException {
        final ExternalId projectExternalId = new SimpleBdioFactory().createNameVersionExternalId(packageManagerEnum.getForge(), projectName, version);
        final SimpleBdioDocument bdioDocument = new SimpleBdioFactory().createSimpleBdioDocument(codeLocationName, projectName, version, projectExternalId);
        final MutableDependencyGraph dependencies = new SimpleBdioFactory().createMutableDependencyGraph();

        extractComponents(dependencies, dockerImageRepo, dockerImageTag, architecture, executor.runPackageManager(imagePkgMgr), preferredAliasNamespace);
        logger.info(String.format("Found %s potential components", dependencies.getRootDependencies().size()));

        new SimpleBdioFactory().populateComponents(bdioDocument, projectExternalId, dependencies);
        return bdioDocument;
    }

    private void addDependency(final MutableDependencyGraph dependencies, final String name, final String version, final String arch, final Forge forge) {
        final ExternalId extId = new SimpleBdioFactory().createArchitectureExternalId(forge, name, version, arch);
        final Dependency dep = new SimpleBdioFactory().createDependency(name, version, extId); // createDependencyNode(forge, name, version, arch);
        logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dep.name, dep.externalId.createBdioId()));
        dependencies.addChildToRoot(dep);
    }
}
