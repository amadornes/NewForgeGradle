package net.minecraftforge.gradle.plugin;

import net.minecraftforge.gradle.util.Remapper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static net.minecraftforge.gradle.Constants.*;

/**
 * The main plugin class.
 */
public final class ForgeGradlePlugin implements Plugin<Project> {

    /**
     * Apply ForgeGradle to the specified project.
     */
    @Override
    public void apply(Project project) {
        ForgeGradleExtension ext = project.getExtensions().create(EXTENSION_NAME, ForgeGradleExtension.class, project, project.getObjects());

        // Add a deobf() method to the dependencies block
        Remapper.addDeobfMethod(project);

        // If we need to reobfuscate the jar, schedule it after it's been built
        if (ext.reobfuscateJar) {
            // project.task(TASK_BUILD).finalizedBy(TASK_REOBFUSCATE);
        }
    }

}
