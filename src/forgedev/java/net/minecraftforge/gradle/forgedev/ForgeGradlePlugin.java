package net.minecraftforge.gradle.forgedev;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * The main plugin class.
 */
public final class ForgeGradlePlugin implements Plugin<Project> {

    /**
     * Apply ForgeGradle to the specified project.
     */
    @Override
    public void apply(Project project) {
        ForgeGradlePluginInstance inst = new ForgeGradlePluginInstance(project);
        inst.init();
        inst.initExtensions();
        project.afterEvaluate($ -> inst.afterEvaluate());
    }

}
