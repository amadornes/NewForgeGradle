package net.minecraftforge.gradle.shared.mappings;

import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.minecraftforge.gradle.api.mapping.MappingManager;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.util.LazyFileCollection;
import net.minecraftforge.gradle.shared.util.RemappedDependency;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.impldep.com.beust.jcommander.internal.Maps;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that handles remapping jar files and project dependencies.
 */
public class Remapper {

    public static Set<File> remap(MappingManager manager, MappingVersion mapping, Set<File> files) {
        Set<File> results = new HashSet<>();
        File mappingFile = manager.getMapping(mapping);
        String mappingHash = manager.getMappingHash(mapping);

        for (File file : files) {
            String path = file.getAbsolutePath();
            File deobfFile = new File(path.substring(0, path.lastIndexOf('.')) + "-remapped-"
                    + mapping.getProvider() + "-" + mappingHash + ".jar");
            results.add(deobfFile);

            try {
                Util.applySpecialSource(file, deobfFile, jar -> {
                    JointProvider inheritanceProvider = new JointProvider();
                    inheritanceProvider.add(new JarProvider(jar));
                    return createRemapper(Collections.singleton(mappingFile), inheritanceProvider);
                });
            } catch (IOException ex) {
                throw new RuntimeException("Failed to remap file!", ex);
            }
        }
        return results;
    }

    /**
     * Creates a jar remapper that uses the specified mapping files and handles
     * inheritance through the provider, if specified.
     *
     * @throws IOException If a mapping could not be loaded.
     */
    public static JarRemapper createRemapper(Iterable<File> mappings, InheritanceProvider inheritanceProvider) throws IOException {
        // Create mapping object
        JarMapping mapping = new JarMapping();

        // Load mappings
        for (File file : mappings) {
            mapping.loadMappings(file);
        }

        // Allow inheritance from other classes in the jar as well as the classpath
        if (inheritanceProvider != null) {
            mapping.setFallbackInheritanceProvider(inheritanceProvider);
        }

        // Create the remapper
        return new JarRemapper(null, mapping);
    }

    /**
     * Replaces any temporary {@link RemappedDependency} in the project
     * with the correct remapped version of that dependency.
     */
    public static void fixDependencies(Project project, MappingManager manager) {
        for (Configuration cfg : project.getConfigurations()) {
            DependencySet dependencies = cfg.getDependencies();
            Map<Dependency, Dependency> replacements = new HashMap<>();
            for (Dependency dep : dependencies) {
                if (!(dep instanceof RemappedDependency)) continue;
                RemappedDependency remDep = (RemappedDependency) dep;
                if (remDep.getDependency() instanceof FileCollectionDependency) {
                    Set<File> files = ((FileCollectionDependency) remDep.getDependency()).getFiles().getFiles();
                    FileCollection remapped = new LazyFileCollection("deobf dep", () -> remap(manager, remDep.getMapping(), files));
                    replacements.put(dep, project.getDependencies().create(remapped));
                } else if (remDep.getDependency() instanceof ModuleDependency) {
                    ModuleDependency moduleDep = (ModuleDependency) remDep.getDependency();
                    MappingVersion mapping = remDep.getMapping();

                    String newGroup = "remapped." + mapping.getProvider() + "." + mapping.getChannel() + "."
                            + mapping.getVersion() + "." + mapping.getMapping() + "." + remDep.getDependency().getGroup();

                    ModuleDependency newDep = (ModuleDependency) project.getDependencies().create(Maps.newHashMap(
                            "group", newGroup,
                            "name", remDep.getDependency().getName(),
                            "version", remDep.getDependency().getVersion(),
                            "transitive", moduleDep.isTransitive()
                    ));
                    moduleDep.getArtifacts().forEach(newDep::addArtifact);

                    replacements.put(dep, newDep);
                }
            }
            dependencies.removeAll(replacements.keySet());
            dependencies.addAll(replacements.values());
        }
    }
}
