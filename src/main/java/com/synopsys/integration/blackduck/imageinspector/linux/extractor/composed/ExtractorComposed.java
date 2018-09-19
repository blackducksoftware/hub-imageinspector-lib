package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;

public class ExtractorComposed {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExtractorBehavior extractorBehavior;
    private final SimpleBdioFactory simpleBdioFactory;
    private final ImagePkgMgrDatabase imagePkgMgrDatabase;

    public ExtractorComposed(final SimpleBdioFactory simpleBdioFactory, final ExtractorBehavior extractorBehavior, final ImagePkgMgrDatabase imagePkgMgrDatabase) {
        this.extractorBehavior = extractorBehavior;
        this.simpleBdioFactory = simpleBdioFactory;
        this.imagePkgMgrDatabase = imagePkgMgrDatabase;
    }

    public final SimpleBdioDocument extract(final String dockerImageRepo, final String dockerImageTag, final String architecture, final String codeLocationName, final String projectName,
            final String projectVersion,
            final String preferredAliasNamespace)
            throws IntegrationException, IOException, InterruptedException {

        final SimpleBdioDocument bdioDocument = extractBdio(dockerImageRepo, dockerImageTag, architecture, codeLocationName, projectName, projectVersion, preferredAliasNamespace);
        return bdioDocument;
    }

    private SimpleBdioDocument extractBdio(final String dockerImageRepo, final String dockerImageTag, final String architecture, final String codeLocationName, final String projectName,
            final String version,
            final String preferredAliasNamespace)
            throws IntegrationException, IOException, InterruptedException {
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(extractorBehavior.getPackageManagerEnum().getForge(), projectName, version);
        final SimpleBdioDocument bdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, version, projectExternalId);

        final List<ComponentDetails> comps = extractorBehavior.extractComponents(dockerImageRepo, dockerImageTag, architecture, extractorBehavior.getPkgMgrExecutor().runPackageManager(imagePkgMgrDatabase), preferredAliasNamespace);
        final MutableDependencyGraph dependencies = generateDependencies(comps);
        logger.info(String.format("Found %s potential components", dependencies.getRootDependencies().size()));

        simpleBdioFactory.populateComponents(bdioDocument, projectExternalId, dependencies);
        return bdioDocument;
    }

    private MutableDependencyGraph generateDependencies(final List<ComponentDetails> comps) {
        final MutableDependencyGraph dependencies = simpleBdioFactory.createMutableDependencyGraph();
        for (final ComponentDetails comp : comps) {
            final String forgeId = String.format("@%s", comp.getPreferredAliasNamespace());
            logger.debug(String.format("Generating component with preferred alias namespace (forge=@LinuxDistro): %s", forgeId));
            final Forge preferredNamespaceForge = new Forge("/", "/", forgeId);
            addDependency(dependencies, comp.getName(), comp.getVersion(), comp.getArchitecture(), preferredNamespaceForge);
        }
        return dependencies;
    }

    private void addDependency(final MutableDependencyGraph dependencies, final String name, final String version, final String arch, final Forge forge) {
        final ExternalId extId = simpleBdioFactory.createArchitectureExternalId(forge, name, version, arch);
        final Dependency dep = simpleBdioFactory.createDependency(name, version, extId); // createDependencyNode(forge, name, version, arch);
        logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dep.name, dep.externalId.createBdioId()));
        dependencies.addChildToRoot(dep);
    }
}
