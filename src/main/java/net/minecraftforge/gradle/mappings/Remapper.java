package net.minecraftforge.gradle.mappings;

import groovy.lang.Closure;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.minecraftforge.gradle.Constants;
import net.minecraftforge.gradle.api.MappingEntry;
import net.minecraftforge.gradle.api.MappingProvider;
import net.minecraftforge.gradle.api.MappingVersion;
import net.minecraftforge.gradle.util.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
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
    public static Object resolveDeobf(Project project, Dependency dependency) {
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

    /**
     * Gets or creates a mappings file for the specified provider and version.
     * <p>
     * Automatically handles refreshing dependencies.
     */
    public static File getMappings(Project project, MappingProvider provider, MappingVersion version) {
        File gradleHome = project.getGradle().getGradleUserHomeDir();
        File mappingsDir = new File(gradleHome, Constants.CACHE_GENERATED_MAPPINGS_DIR + "/" + version.getProvider());
        File versionDir = new File(mappingsDir, version.getMCVersion() + "-" + version.getChannel() + "_" + version.getVersion());
        File file = new File(versionDir, version.getMapping() + ".srg");

        if (!file.exists() || project.getGradle().getStartParameter().isRefreshDependencies()) {
            Map<Object, Object> dependencies = provider.getDependencies(version);
            Map<MappingEntry, MappingEntry> mappings = provider.getMapping(version,
                    name -> Util.resolveDependency(project, dependencies.get(name)).iterator().next());

            try {
                if (file.exists()) file.delete();
                file.getParentFile().mkdirs();
                file.createNewFile();

                PrintWriter pw = new PrintWriter(file);
                mappings.forEach((from, to) -> pw.println(serializeMapping(from, to)));
                pw.flush();
                pw.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return file;
    }

    /**
     * Converts a pair of mapping entries into an SRG-formatted string.
     */
    private static String serializeMapping(MappingEntry from, MappingEntry to) {
        if (from instanceof MappingEntry.Package) {
            return "PK: " + from.getName() + " " + to.getName();
        } else if (from instanceof MappingEntry.Class) {
            return "CL: " + from.getName() + " " + to.getName();
        } else if (from instanceof MappingEntry.Field) {
            MappingEntry.Field f1 = (MappingEntry.Field) from;
            MappingEntry.Field f2 = (MappingEntry.Field) to;
            return "FD: "
                    + f1.getOwner() + "/" + f1.getName() + " "
                    + f2.getOwner() + "/" + f2.getName();
        } else if (from instanceof MappingEntry.Method) {
            MappingEntry.Method m1 = (MappingEntry.Method) from;
            MappingEntry.Method m2 = (MappingEntry.Method) to;
            return "MD: "
                    + m1.getOwner() + "/" + m1.getName() + " " + m1.getDescriptor() + " "
                    + m2.getOwner() + "/" + m2.getName() + " " + m2.getDescriptor();
        }
        throw new IllegalArgumentException("Invalid mapping entry type: " + from.getClass().getName());
    }

}
