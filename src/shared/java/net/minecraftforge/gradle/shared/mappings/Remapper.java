package net.minecraftforge.gradle.shared.mappings;

import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.util.DependencyResolver;
import net.minecraftforge.gradle.shared.util.IOSupplier;
import net.minecraftforge.gradle.shared.util.LazyFileCollection;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.Pair;
import org.gradle.internal.hash.HashUtil;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.impldep.com.beust.jcommander.internal.Maps;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
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

    public static Pair<IOSupplier<byte[]>, HashValue> lazyRemapBytes(DependencyResolver dependencyResolver, MappingVersion mapping, File file) {
        Pair<IOSupplier<File>, HashValue> tmp = lazyRemapTmp(dependencyResolver, mapping, file);
        return Pair.of(() -> {
            File tmpFile = tmp.getLeft().get();
            FileInputStream fis = new FileInputStream(tmpFile);
            byte[] bytes = IOUtils.toByteArray(fis);
            fis.close();
            tmpFile.delete();
            return bytes;
        }, tmp.getRight());
    }

    public static Pair<IOSupplier<File>, HashValue> lazyRemapTmp(DependencyResolver dependencyResolver, MappingVersion mapping, File file) {
        File mappingFile = dependencyResolver.resolveDependency(mapping.asMavenArtifactName()).iterator().next();
        return Pair.of(() -> {
            File tmp = File.createTempFile("remap", null);
            Util.applySpecialSource(file, tmp, jar -> {
                JointProvider inheritanceProvider = new JointProvider();
                inheritanceProvider.add(new JarProvider(jar));
                return createRemapper(Collections.singleton(mappingFile), inheritanceProvider);
            });
            return tmp;
        }, HashUtil.sha1(mappingFile));
    }

    public static Set<File> remap(DependencyResolver dependencyResolver, MappingVersion mapping, Set<File> files) {
        Set<File> results = new HashSet<>();
        File mappingFile = dependencyResolver.resolveDependency(mapping.asMavenArtifactName()).iterator().next();
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
    public static void fixDependencies(Project project, DependencyResolver dependencyResolver) {
        for (Configuration cfg : project.getConfigurations()) {
            DependencySet dependencies = cfg.getDependencies();
            Map<Dependency, Dependency> replacements = new HashMap<>();
            for (Dependency dep : dependencies) {
                if (!(dep instanceof RemappedDependency)) continue;
                RemappedDependency remDep = (RemappedDependency) dep;
                if (remDep.getDependency() instanceof FileCollectionDependency) {
                    Set<File> files = ((FileCollectionDependency) remDep.getDependency()).getFiles().getFiles();
                    FileCollection remapped = new LazyFileCollection("deobf dep", () -> remap(dependencyResolver, remDep.getMapping(), files));
                    replacements.put(dep, project.getDependencies().create(remapped));
                } else if (remDep.getDependency() instanceof ModuleDependency) {
                    ModuleDependency moduleDep = (ModuleDependency) remDep.getDependency();
                    MappingVersion mapping = remDep.getMapping();

                    String newGroup = "remap." + mapping.getMapping() + "." + remDep.getDependency().getGroup();

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
