package net.minecraftforge.gradle.moddev;

import net.minecraftforge.gradle.api.moddev.ForgeGradleAPI;
import net.minecraftforge.gradle.shared.Constants;
import net.minecraftforge.gradle.shared.mappings.MappingManagerImpl;
import net.minecraftforge.gradle.shared.mappings.Remapper;
import net.minecraftforge.gradle.shared.repo.MappingRepo;
import net.minecraftforge.gradle.shared.repo.RemappingRepo;
import net.minecraftforge.gradle.shared.util.DependencyResolver;
import org.gradle.api.Project;

/**
 * A single instance of the ForgeGradle plugin.
 */
public class ForgeGradlePluginInstance {

    public final Project project;
    public final ForgeGradleAPI api = new ForgeGradleAPIImpl(this);

    // Internal systems
    public final DependencyResolver dependencyResolver;
    public final MappingManagerImpl mappings;

    // Extensions
    public ForgeGradleExtension fgExt;

    ForgeGradlePluginInstance(Project project) {
        this.project = project;
        this.dependencyResolver = new DependencyResolver(project);
        this.mappings = new MappingManagerImpl(project, dependencyResolver);

    }

    public void init() {
        ForgeGradleDSL.extendDSL(this);
    }

    public void initExtensions() {
        fgExt = project.getExtensions().create(Constants.FORGE_GRADLE_EXTENSION_NAME, ForgeGradleExtension.class, project);
    }

    public void afterEvaluate() {
        mappings.addRepositories();
        MappingRepo.add(project, mappings, fgExt.minecraft.version, "mappings", "https://amadorn.es");
        RemappingRepo.add(project, dependencyResolver, fgExt.mappings.provider, fgExt.mappings.channel, fgExt.mappings.version,
                fgExt.minecraft.version, "remapping", "https://amadornes.com");
        Remapper.fixDependencies(project, dependencyResolver);
    }

}
