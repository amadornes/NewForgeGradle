package net.minecraftforge.gradle.forgedev;

import org.gradle.api.Project;

/**
 * A single instance of the ForgeGradle plugin.
 */
public class ForgeGradlePluginInstance {

    public final Project project;

    // Internal systems
//    public final MappingManagerImpl mappings;

    ForgeGradlePluginInstance(Project project) {
        this.project = project;
    }

    public void init() {
    }

    public void initExtensions() {
    }

    public void afterEvaluate() {
//        mappings.addRepositories();
    }

}
