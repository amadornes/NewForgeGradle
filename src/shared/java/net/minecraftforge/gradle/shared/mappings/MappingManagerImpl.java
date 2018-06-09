package net.minecraftforge.gradle.shared.mappings;

import net.minecraftforge.gradle.api.mapping.MappingEntry;
import net.minecraftforge.gradle.api.mapping.MappingManager;
import net.minecraftforge.gradle.api.mapping.MappingProvider;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.Constants;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.Project;
import org.gradle.internal.impldep.org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MappingManagerImpl implements MappingManager {

    private final Project project;
    private final AtomicInteger counter;

    private final Map<String, MappingProvider> mappingProviders = new HashMap<>();

    public MappingManagerImpl(Project project, AtomicInteger counter) {
        this.project = project;
        this.counter = counter;
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

    @Override
    public File getMapping(MappingVersion version) {
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

        File file = getMappingPath(version);

        if (!file.exists() || project.getGradle().getStartParameter().isRefreshDependencies()) {
            Map<Object, Object> dependencies = provider.getDependencies(version);
            Map<MappingEntry, MappingEntry> mappings = provider.getMapping(version,
                    name -> Util.resolveDependency(project, counter, dependencies.get(name)).iterator().next());

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

    @Override
    public String getMappingHash(MappingVersion version) {
        try {
            File mapping = getMapping(version);
            FileInputStream fis = new FileInputStream(mapping);
            String md5 = DigestUtils.md5Hex(fis);
            fis.close();
            return md5;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private File getMappingPath(MappingVersion version) {
        File gradleHome = project.getGradle().getGradleUserHomeDir();
        File mappingsDir = new File(gradleHome, Constants.CACHE_GENERATED_MAPPINGS_DIR + "/" + version.getProvider());
        File versionDir = new File(mappingsDir, version.getMCVersion() + "-" + version.getChannel() + "_" + version.getVersion());
        return new File(versionDir, version.getMapping() + ".srg");
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
