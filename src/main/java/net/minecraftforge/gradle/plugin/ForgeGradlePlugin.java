package net.minecraftforge.gradle.plugin;

import net.minecraftforge.gradle.api.MappingProvider;
import net.minecraftforge.gradle.mappings.MCPMappingProvider;
import net.minecraftforge.gradle.mappings.Remapper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

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
        // Add ForgeGradle extension
        ForgeGradleExtension fgExt = project.getExtensions().create(FORGE_GRADLE_EXTENSION_NAME, ForgeGradleExtension.class, project);

        // Add mappings extension (for registering custom providers)
        Map<String, MappingProvider> mappingProviders = new HashMap<>();
        mappingProviders.put("mcp", new MCPMappingProvider());
        project.getExtensions().create(MAPPINGS_EXTENSION_NAME, MappingsExtension.class, mappingProviders);
        project.afterEvaluate($ -> {
            // Do this after evaluation since buildscripts may add custom mapping providers
            for (MappingProvider provider : mappingProviders.values()) {
                provider.addDependencyRepositories(project.getRepositories());
            }
        });

        // Add a deobf() method to the dependencies block
        Remapper.addDeobfMethod(project);

        // If we need to reobfuscate the jar, schedule it after it's been built
        if (fgExt.reobfuscateJar) {
            // project.task(TASK_BUILD).finalizedBy(TASK_REOBFUSCATE);
        }
    }

}
