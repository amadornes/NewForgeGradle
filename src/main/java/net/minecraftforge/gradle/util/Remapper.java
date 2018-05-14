package net.minecraftforge.gradle.util;

import groovy.lang.Closure;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * Utility class that handles remapping jar files and project dependencies.
 */
public class Remapper {

    /**
     * Adds a {@code deobf(...)} method to the {@code dependencies} block of
     * the buildscript, allowing for dynamic dependency deobfuscation.
     */
    public static void addDeobfMethod(Project project) {
        DependencyHandler deps = project.getDependencies();
        Util.addMethod(deps, "deobf", new Closure<Object>(deps) {
            public Object doCall(Object param) {
                return resolveDeobf(project, deps.create(param));
            }
        });
    }

    /**
     *
     */
    private static Object resolveDeobf(Project project, Dependency dependency) {
        // Resolve the requested dependency TODO: do lazily inside the returned FileCollection
        Set<File> files = Util.resolveDependency(project, dependency);

        // Just return the dependency for now TODO: create and return lazy FileCollection
        return dependency;
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
