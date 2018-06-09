package net.minecraftforge.gradle.api.moddev;

import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;

/**
 * Public-facing interface to access the ForgeGradle API for a specific project.
 * <p>
 * Get an instance of this class through the project's {@link PluginContainer}.
 */
public interface FGPlugin {

    /**
     * Gets the instance of the ForgeGradle API for the specified project.
     */
    ForgeGradleAPI getAPI(Project project);

}
