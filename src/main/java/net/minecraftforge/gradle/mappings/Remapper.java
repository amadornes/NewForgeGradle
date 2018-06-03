package net.minecraftforge.gradle.mappings;

import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.plugin.ForgeGradleExtension;
import net.minecraftforge.gradle.plugin.ForgeGradlePluginInstance;
import net.minecraftforge.gradle.util.LazyFileCollection;
import net.minecraftforge.gradle.util.Util;
import org.gradle.api.artifacts.Dependency;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Utility class that handles remapping jar files and project dependencies.
 */
public class Remapper {

    public static Object deobfDependency(ForgeGradlePluginInstance fg, Dependency dependency) {
        return deobfDependency(fg, () -> dependency);
    }

    public static Object deobfDependency(ForgeGradlePluginInstance fg, Supplier<Dependency> dependencySupplier) {
        return remapDependency(fg, dependencySupplier, () -> {
            ForgeGradleExtension.Mappings fgMappings = fg.fgExt.mappings;
            return new MappingVersion(fgMappings.provider, fgMappings.channel, fgMappings.version,
                    fg.fgExt.minecraft.version, fgMappings.deobfMappings);
        }, true);
    }

    public static Object remapDependency(ForgeGradlePluginInstance fg, Dependency dependency, Map<String, Object> mappingInfo) {
        return remapDependency(fg, () -> dependency, mappingInfo);
    }

    public static Object remapDependency(ForgeGradlePluginInstance fg, Supplier<Dependency> dependencySupplier, Map<String, Object> mappingInfo) {
        return remapDependency(fg, dependencySupplier, () -> {
            ForgeGradleExtension.Mappings fgMappings = fg.fgExt.mappings;
            String provider = (String) mappingInfo.getOrDefault("provider", fgMappings.provider);
            String channel = (String) mappingInfo.getOrDefault("channel", fgMappings.channel);
            String mcversion = (String) mappingInfo.getOrDefault("mcversion", fg.fgExt.minecraft.version);
            String version = (String) mappingInfo.getOrDefault("version", fgMappings.version);
            String mapping = (String) mappingInfo.get("mapping");
            if (mapping == null)
                throw new IllegalArgumentException("Attempted to remap dependency without specifying mappings!");
            return new MappingVersion(provider, channel, version, mcversion, mapping);
        }, (boolean) mappingInfo.getOrDefault("remapTransitives", true));
    }

    /**
     * Creates a dependency object that represents the deobfuscated version
     */
    public static Object remapDependency(ForgeGradlePluginInstance fg, Supplier<Dependency> dependencySupplier, Supplier<MappingVersion> mappingSupplier, boolean remapTransitives) {
        return new LazyFileCollection("deobf dependency", () -> {
            // Resolve the requested dependency
            Dependency dep = dependencySupplier.get();
            Set<File> files = Util.resolveDependency(fg, dep);
            Set<File> deobfed = new HashSet<>();

            if (!remapTransitives) {
                Dependency nonTransitive = Util.asNonTransitive(dep);
                Set<File> nonTransitiveFiles = Util.resolveDependency(fg, nonTransitive);
                deobfed.addAll(files);
                deobfed.removeAll(nonTransitiveFiles);
                files = nonTransitiveFiles;
            }

            MappingVersion mappingVersion = mappingSupplier.get();
            File mappingFile = fg.mappings.getMapping(mappingVersion);
            String mappingHash = fg.mappings.getMappingHash(mappingVersion);

            for (File file : files) {
                String path = file.getAbsolutePath();
                File deobfFile = new File(path.substring(0, path.lastIndexOf('.')) + "-deobf-"
                        + mappingVersion.getProvider() + "-" + mappingHash + ".jar");
                deobfed.add(deobfFile);

                // Skip deobfuscation if we've already visited this file
                if (deobfFile.exists() && (!fg.project.getGradle().getStartParameter().isRefreshDependencies() || !fg.refreshedDeps.add(deobfFile))) {
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
