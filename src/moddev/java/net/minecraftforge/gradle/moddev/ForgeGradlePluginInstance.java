package net.minecraftforge.gradle.moddev;

import net.minecraftforge.gradle.api.moddev.ForgeGradleAPI;
import net.minecraftforge.gradle.shared.Constants;
import net.minecraftforge.gradle.shared.mappings.MappingManagerImpl;
import org.gradle.api.Project;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A single instance of the ForgeGradle plugin.
 */
public class ForgeGradlePluginInstance {

    public final Project project;
    public final ForgeGradleAPI api = new ForgeGradleAPIImpl(this);

    // Internal systems
    public final MappingManagerImpl mappings;

    // Extensions
    public ForgeGradleExtension fgExt;

    // Miscellaneous
    public AtomicInteger dependencyID = new AtomicInteger(0); // Stores the current dependency ID
    public Set<File> refreshedDeps = new HashSet<>();

    ForgeGradlePluginInstance(Project project) {
        this.project = project;

        this.mappings = new MappingManagerImpl(project, dependencyID);
    }

    public void init() {
        // It's a pain, but we need to get the class at runtime and call the method.
        // Java compiles first, then groovy, so the groovy class we're accessing here
        // isn't available when the class is compiled, resulting in an error.
        // We're just calling a method, though, so it should be fine.
        try {
            Class<?> clazz = Class.forName("net.minecraftforge.gradle.shared.groovy.ForgeGradleDSL");
            clazz.getMethod("extendDSL", ForgeGradlePluginInstance.class).invoke(null, this);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void initExtensions() {
        fgExt = project.getExtensions().create(Constants.FORGE_GRADLE_EXTENSION_NAME, ForgeGradleExtension.class, project);
    }

    public void afterEvaluate() {
        mappings.addRepositories();
    }

}
