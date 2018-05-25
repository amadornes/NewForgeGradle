package net.minecraftforge.gradle.plugin;

import net.minecraftforge.gradle.mappings.MCPMappingProvider;
import net.minecraftforge.gradle.mappings.MappingManager;
import net.minecraftforge.gradle.mappings.Remapper;
import org.gradle.api.Project;

import static net.minecraftforge.gradle.Constants.*;

/**
 * A single instance of the ForgeGradle plugin.
 */
public class ForgeGradlePluginInstance {

    public final Project project;

    // Internal systems
    public final MappingManager mappings;

    // Extensions
    public ForgeGradleExtension fgExt;

    ForgeGradlePluginInstance(Project project) {
        this.project = project;

        this.mappings = new MappingManager(this);
    }

    public void init() {
        // Add a deobf() method to the dependencies block
        Remapper.addDeobfMethod(this);
    }

    public void initExtensions() {
        fgExt = project.getExtensions().create(FORGE_GRADLE_EXTENSION_NAME, ForgeGradleExtension.class, project);
        project.getExtensions().create(MAPPINGS_EXTENSION_NAME, MappingsExtension.class, mappings);
    }

    public void afterEvaluate() {
        if (fgExt.builtin.mcpMappings) {
            mappings.register("mcp", new MCPMappingProvider());
        }
        mappings.addRepositories();
    }

}
