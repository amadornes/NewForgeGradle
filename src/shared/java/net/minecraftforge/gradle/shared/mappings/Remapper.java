package net.minecraftforge.gradle.shared.mappings;

import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.minecraftforge.gradle.api.mapping.MappingManager;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.util.LazyFileCollection;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Utility class that handles remapping jar files and project dependencies.
 */
public class Remapper {

    public static Object deobfDependency(Project project, AtomicInteger counter, MappingManager manager, Set<File> refreshedDeps, MappingVersion mapping, Dependency dependency) {
        return remapDependency(project, counter, manager, refreshedDeps, mapping, () -> dependency, true);
    }

    public static Object remapDependency(Project project, AtomicInteger counter, MappingManager manager, Set<File> refreshedDeps, Map<String, Object> mappingInfo, Dependency dependency) {
        return remapDependencyWithDefaults(project, counter, manager, refreshedDeps, null, mappingInfo, () -> dependency);
    }

    public static Object remapDependencyWithDefaults(Project project, AtomicInteger counter, MappingManager manager, Set<File> refreshedDeps, MappingVersion defaults, Map<String, Object> mappingInfo, Supplier<Dependency> dependencySupplier) {
        return remapDependency(project, counter, manager, refreshedDeps, MappingVersion.lazy(() -> {
            String provider = (String) mappingInfo.getOrDefault("provider", defaults.getProvider());
            String channel = (String) mappingInfo.getOrDefault("channel", defaults.getChannel());
            String mcversion = (String) mappingInfo.getOrDefault("mcversion", defaults.getMCVersion());
            String version = (String) mappingInfo.getOrDefault("version", defaults.getVersion());
            String mapping = (String) mappingInfo.get("mapping");
            if (mapping == null)
                throw new IllegalArgumentException("Attempted to remap dependency without specifying mappings!");
            return new MappingVersion(provider, channel, version, mcversion, mapping);
        }), dependencySupplier, (boolean) mappingInfo.getOrDefault("remapTransitives", true));
    }

    /**
     * Creates a dependency object that represents the deobfuscated version
     */
    public static Object remapDependency(Project project, AtomicInteger counter, MappingManager manager, Set<File> refreshedDeps, MappingVersion mapping, Supplier<Dependency> dependencySupplier, boolean remapTransitives) {
        return new LazyFileCollection("deobf dependency", () -> {
            // Resolve the requested dependency
            Dependency dep = dependencySupplier.get();
            Set<File> files = Util.resolveDependency(project, counter, dep);
            Set<File> deobfed = new HashSet<>();

            if (!remapTransitives) {
                Dependency nonTransitive = Util.asNonTransitive(dep);
                Set<File> nonTransitiveFiles = Util.resolveDependency(project, counter, nonTransitive);
                deobfed.addAll(files);
                deobfed.removeAll(nonTransitiveFiles);
                files = nonTransitiveFiles;
            }

            File mappingFile = manager.getMapping(mapping);
            String mappingHash = manager.getMappingHash(mapping);

            for (File file : files) {
                String path = file.getAbsolutePath();
                File deobfFile = new File(path.substring(0, path.lastIndexOf('.')) + "-deobf-"
                        + mapping.getProvider() + "-" + mappingHash + ".jar");
                deobfed.add(deobfFile);

                // Skip deobfuscation if we've already visited this file
                if (deobfFile.exists() && (!project.getGradle().getStartParameter().isRefreshDependencies() || !refreshedDeps.add(deobfFile))) {
                    continue;
                }

                try {
                    Util.applySpecialSource(file, deobfFile, jar -> {
                        JointProvider inheritanceProvider = new JointProvider();
                        inheritanceProvider.add(new JarProvider(jar));
                        return createRemapper(Collections.singleton(mappingFile), inheritanceProvider);
                    });
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to deobfuscate file!", ex);
                }
            }

            return deobfed;
        });
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

}
