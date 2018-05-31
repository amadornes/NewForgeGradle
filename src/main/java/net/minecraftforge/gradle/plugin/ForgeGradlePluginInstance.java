package net.minecraftforge.gradle.plugin;

import net.minecraftforge.gradle.mappings.MappingManagerImpl;
import net.minecraftforge.gradle.repo.CustomRepo;
import org.gradle.api.Project;

import static net.minecraftforge.gradle.Constants.*;

/**
 * A single instance of the ForgeGradle plugin.
 */
public class ForgeGradlePluginInstance {

    public final Project project;

    // Internal systems
    public final MappingManagerImpl mappings;

    // Extensions
    public ForgeGradleExtension fgExt;

    // Miscellaneous
    public int dependencyID = 0; // Stores the current dependency ID

    ForgeGradlePluginInstance(Project project) {
        this.project = project;

        this.mappings = new MappingManagerImpl(this);

        project.getRepositories().add(new CustomRepo());

        project.setProperty(PLUGIN_API_PROPERTY_NAME, new ForgeGradleAPIImpl(this));
    }

    public void init() {
        // It's a pain, but we need to get the class at runtime and call the method.
        // Java compiles first, then groovy, so the groovy class we're accessing here
        // isn't available when the class is compiled, resulting in an error.
        // We're just calling a method, though, so it should be fine.
        try {
            Class<?> clazz = Class.forName("net.minecraftforge.gradle.groovy.ForgeGradleDSL");
            clazz.getMethod("extendDSL", ForgeGradlePluginInstance.class).invoke(null, this);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void initExtensions() {
        fgExt = project.getExtensions().create(FORGE_GRADLE_EXTENSION_NAME, ForgeGradleExtension.class, project);
    }

    public void afterEvaluate() {
        mappings.addRepositories();
    }

}
