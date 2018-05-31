package net.minecraftforge.gradle.plugin;

import net.minecraftforge.gradle.Constants;
import net.minecraftforge.gradle.api.FGPlugin;
import net.minecraftforge.gradle.api.ForgeGradleAPI;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * The main plugin class.
 */
public final class ForgeGradlePlugin implements Plugin<Project>, FGPlugin {

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

    @Override
    public ForgeGradleAPI getAPI(Project project) {
        return (ForgeGradleAPI) project.getProperties().get(Constants.PLUGIN_API_PROPERTY_NAME);
    }

}
