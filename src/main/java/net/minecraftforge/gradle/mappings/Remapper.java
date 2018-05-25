package net.minecraftforge.gradle.mappings;

import groovy.lang.Closure;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.minecraftforge.gradle.api.MappingVersion;
import net.minecraftforge.gradle.plugin.ForgeGradleExtension;
import net.minecraftforge.gradle.plugin.ForgeGradlePluginInstance;
import net.minecraftforge.gradle.util.LazyFileCollection;
import net.minecraftforge.gradle.util.Util;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Utility class that handles remapping jar files and project dependencies.
 */
public class Remapper {

    /**
     * Adds a {@code deobf(...)} method to the {@code dependencies} block of
     * the buildscript, allowing for dynamic dependency deobfuscation.
     */
    public static void addDeobfMethod(ForgeGradlePluginInstance fg) {
        DependencyHandler deps = fg.project.getDependencies();
        Util.addMethod(deps, "deobf", new Closure<Object>(deps) {
            public Object doCall(Object param) {
                return deobfDependency(fg, deps.create(param));
            }
        });
    }

    public static Object deobfDependency(ForgeGradlePluginInstance fg, Dependency dependency) {
        return remapDependency(fg, dependency, () -> {
            ForgeGradleExtension.Mappings mappings = fg.fgExt.mappings;
            MappingVersion version = new MappingVersion(mappings.provider, mappings.channel, mappings.version,
                    fg.fgExt.minecraft.version, mappings.deobfMappings);
            return version;
        });
    }

    /**
     * Creates a dependency object that represents the deobfuscated version
     */
    public static Object remapDependency(ForgeGradlePluginInstance fg, Dependency dependency, Supplier<MappingVersion> mappingSupplier) {
        return new LazyFileCollection("deobf dependency", () -> {
            // Resolve the requested dependency
            Set<File> files = Util.resolveDependency(fg, dependency);
            Set<File> deobfed = new HashSet<>();

            MappingVersion mappingVersion = mappingSupplier.get();
            File mappingFile = fg.mappings.getMapping(mappingVersion);
            String mappingHash = fg.mappings.getMappingHash(mappingVersion);

            for (File file : files) {
                String path = file.getAbsolutePath();
                File deobfFile = new File(path.substring(0, path.lastIndexOf('.')) + "-deobf-"
                        + mappingVersion.getProvider() + "-" + mappingHash + ".jar");
                deobfed.add(deobfFile);

                if (deobfFile.exists()) continue;

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
