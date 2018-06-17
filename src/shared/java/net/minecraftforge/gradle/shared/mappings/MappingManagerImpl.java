package net.minecraftforge.gradle.shared.mappings;

import net.minecraftforge.gradle.api.mapping.MappingEntry;
import net.minecraftforge.gradle.api.mapping.MappingManager;
import net.minecraftforge.gradle.api.mapping.MappingProvider;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.util.DependencyResolver;
import org.gradle.api.Project;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MappingManagerImpl implements MappingManager {

    private final Project project;
    private final DependencyResolver dependencyResolver;

    private final Map<String, MappingProvider> mappingProviders = new HashMap<>();

    public MappingManagerImpl(Project project, DependencyResolver dependencyResolver) {
        this.project = project;
        this.dependencyResolver = dependencyResolver;
    }

    public void addRepositories() {
        for (MappingProvider provider : mappingProviders.values()) {
            provider.addDependencyRepositories(project.getRepositories());
        }
    }

    @Override
    public void register(MappingProvider provider) {
        mappingProviders.put(provider.getName(), provider);
    }

    @Override
    public boolean isRegistered(String name) {
        return mappingProviders.containsKey(name);
    }

    public byte[] computeMapping(MappingVersion version) {
        MappingProvider provider = mappingProviders.get(version.getProvider());
        if (provider == null) {
            throw new IllegalArgumentException("Could not find requested mapping provider \"" + version.getProvider() + "\".");
        }

        Set<String> supported = provider.getSupportedMappings();
        String mapping = null;
        for (String s : supported) {
            if (s.equals(version.getMapping())) {
                mapping = s;
                break;
            }
        }
        if (mapping == null) {
            throw new IllegalArgumentException("Unsupported mapping type \"" + version.getMapping() + "\".");
        }

        Map<Object, Object> dependencies = provider.getDependencies(version);
        Map<MappingEntry, MappingEntry> mappings = provider.getMapping(version,
                name -> dependencyResolver.resolveDependency(dependencies.get(name)).iterator().next());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        mappings.forEach((from, to) -> pw.println(serializeMapping(from, to)));
        pw.flush();
        pw.close();
        return baos.toByteArray();
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
