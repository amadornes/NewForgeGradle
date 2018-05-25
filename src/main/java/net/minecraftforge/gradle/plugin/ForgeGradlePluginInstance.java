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

    private final Project project;

    // Internal systems
    private final MappingManager mappings;

    // Extensions
    private ForgeGradleExtension fgExt;

    ForgeGradlePluginInstance(Project project) {
        this.project = project;

        this.mappings = new MappingManager(project);
    }

    public void init() {
        // Add a deobf() method to the dependencies block
        Remapper.addDeobfMethod(project);
    }

    public void initExtensions() {
        fgExt = project.getExtensions().create(FORGE_GRADLE_EXTENSION_NAME, ForgeGradleExtension.class, project);
        project.getExtensions().create(MAPPINGS_EXTENSION_NAME, MappingsExtension.class, mappings);
    }

    public void afterEvaluate() {
        if(fgExt.builtin.mcpMappings) {
            mappings.register("mcp", new MCPMappingProvider());
        }
    }

}
