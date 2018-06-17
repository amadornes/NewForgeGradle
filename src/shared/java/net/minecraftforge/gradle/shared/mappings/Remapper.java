package net.minecraftforge.gradle.shared.mappings;

import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.util.LazyFileCollection;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.impldep.com.beust.jcommander.internal.Maps;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class that handles remapping jar files and project dependencies.
 */
public class Remapper {

    public static byte[] remapBytes(Project project, AtomicInteger dependencyID, MappingVersion mapping, File file) throws IOException {
        File tmp = remapTmp(project, dependencyID, mapping, file);
        FileInputStream fis = new FileInputStream(tmp);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(fis, baos);
        baos.close();
        fis.close();
        return baos.toByteArray();
    }

    public static File remapTmp(Project project, AtomicInteger dependencyID, MappingVersion mapping, File file) throws IOException {
        File mappingFile = Util.resolveDependency(project, dependencyID, mapping.asMavenArtifactName()).iterator().next();
        File tmp = File.createTempFile("remap", null);
        Util.applySpecialSource(file, tmp, jar -> {
            JointProvider inheritanceProvider = new JointProvider();
            inheritanceProvider.add(new JarProvider(jar));
            return createRemapper(Collections.singleton(mappingFile), inheritanceProvider);
        });
        return tmp;
    }

    public static Set<File> remap(Project project, AtomicInteger dependencyID, MappingVersion mapping, Set<File> files) {
        Set<File> results = new HashSet<>();
        File mappingFile = Util.resolveDependency(project, dependencyID, mapping.asMavenArtifactName()).iterator().next();
        String mappingHash = mappingFile.getParentFile().getName();

        for (File file : files) {
            if (!file.getAbsolutePath().endsWith(".jar")) {
                results.add(file);
                continue;
            }

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
    public static void fixDependencies(Project project, AtomicInteger dependencyID) {
        for (Configuration cfg : project.getConfigurations()) {
            DependencySet dependencies = cfg.getDependencies();
            Map<Dependency, Dependency> replacements = new HashMap<>();
            for (Dependency dep : dependencies) {
                if (!(dep instanceof RemappedDependency)) continue;
                RemappedDependency remDep = (RemappedDependency) dep;
                if (remDep.getDependency() instanceof FileCollectionDependency) {
                    Set<File> files = ((FileCollectionDependency) remDep.getDependency()).getFiles().getFiles();
                    FileCollection remapped = new LazyFileCollection("deobf dep", () -> remap(project, dependencyID, remDep.getMapping(), files));
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
