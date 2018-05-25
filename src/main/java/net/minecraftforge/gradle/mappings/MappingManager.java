package net.minecraftforge.gradle.mappings;

import net.minecraftforge.gradle.Constants;
import net.minecraftforge.gradle.api.MappingEntry;
import net.minecraftforge.gradle.api.MappingProvider;
import net.minecraftforge.gradle.api.MappingVersion;
import net.minecraftforge.gradle.util.Util;
import org.gradle.api.Project;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MappingManager {

    private final Project project;
    private final Map<String, MappingProvider> mappingProviders = new HashMap<>();

    public MappingManager(Project project) {
        this.project = project;
        // After the project has been evaluated, add all repos
        project.afterEvaluate($ -> {
            for (MappingProvider provider : mappingProviders.values()) {
                provider.addDependencyRepositories(project.getRepositories());
            }
        });
    }

    /**
     * Registers a mapping provider.
     */
    public void register(String name, MappingProvider provider) {
        mappingProviders.put(name, provider);
    }

    /**
     * Gets or creates a mappings file for the specified provider and version.
     * <p>
     * Automatically handles refreshing dependencies.
     */
    public File getMapping(MappingVersion version) {
        MappingProvider provider = mappingProviders.get(version.getProvider());
        if (provider == null) {
            throw new IllegalArgumentException("Could not find requested mapping provider \"" + version.getProvider() + "\".");
        }

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
