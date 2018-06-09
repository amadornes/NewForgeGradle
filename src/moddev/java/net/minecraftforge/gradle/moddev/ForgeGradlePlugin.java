package net.minecraftforge.gradle.moddev;

import net.minecraftforge.gradle.api.moddev.FGPlugin;
import net.minecraftforge.gradle.api.moddev.ForgeGradleAPI;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The main plugin class.
 */
public final class ForgeGradlePlugin implements Plugin<Project>, FGPlugin {

    private final Map<Project, ForgeGradlePluginInstance> instances = new WeakHashMap<>();

    /**
     * Apply ForgeGradle to the specified project.
     */
    @Override
    public void apply(Project project) {
        ForgeGradlePluginInstance inst = new ForgeGradlePluginInstance(project);
        inst.init();
        inst.initExtensions();
        project.afterEvaluate($ -> inst.afterEvaluate());

        instances.put(project, inst);
    }

    @Override
    public ForgeGradleAPI getAPI(Project project) {
        return instances.get(project).api;
    }

}
