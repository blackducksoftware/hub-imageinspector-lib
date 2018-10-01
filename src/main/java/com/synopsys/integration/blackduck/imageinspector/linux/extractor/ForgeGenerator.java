package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import com.synopsys.integration.hub.bdio.model.Forge;

public class ForgeGenerator {
    public static Forge createProjectForge(final String linuxDistroName) {
        return new Forge("/", "/", linuxDistroName);
    }

    public static Forge createComponentForge(final String linuxDistroName) {
        final String preferredNamespaceForgeId = String.format("@%s", linuxDistroName);
        final Forge preferredNamespaceForge = new Forge("/", "/", preferredNamespaceForgeId);
        return preferredNamespaceForge;
    }
}
