package net.minecraftforge.gradle.tasks;

import com.google.common.base.Preconditions;
import net.md_5.specialsource.Jar;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.minecraftforge.gradle.Util;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.function.BooleanSupplier;

/**
 * Task that takes a jar, applies a set of mappings to it, and outputs another jar.<br/>
 * Supports conditional remapping.
 */
public class RemapJarTask extends SpecialSourceTask {

    @InputFiles
    private FileCollection mappings;

    @Input
    private BooleanSupplier condition = () -> true;

    public RemapJarTask() {
        this.mappings = getProject().files();
    }

    @Override
    protected boolean isUpToDate() {
        return !condition.getAsBoolean();
    }

    @Override
    protected void validate() {
        Preconditions.checkArgument(!mappings.isEmpty(), "Mappings are not defined!");
    }

    @Override
    protected JarRemapper createRemapper(Jar inputJar, @Nullable FileCollection classpath) throws IOException {
        // Create mapping object
        JarMapping mapping = new JarMapping();

        // Load mappings
        for (File file : mappings) {
            mapping.loadMappings(file);
        }

        // Allow inheritance from other classes in the jar as well as the classpath
        JointProvider inheritanceProviders = new JointProvider();
        inheritanceProviders.add(new JarProvider(inputJar));
        if (classpath != null) {
            inheritanceProviders.add(new ClassLoaderProvider(new URLClassLoader(Util.toURLs(classpath))));
        }
        mapping.setFallbackInheritanceProvider(inheritanceProviders);

        // Create the remapper
        return new JarRemapper(null, mapping);
    }

    public void mappings(FileCollection files) {
        mappings = mappings.plus(files);
    }

    public void mappings(File... files) {
        mappings(getProject().files((Object[]) files));
    }

    public void mappings(String... files) {
        mappings(getProject().files((Object[]) files));
    }

    public void mappings(String mappings) {

    }

    public void remapWhen(BooleanSupplier condition) {
        this.condition = condition;
    }

    public BooleanSupplier outputMissing() {
        return () -> !output.exists();
    }

    public BooleanSupplier inputModified() {
        return () -> !output.exists() || output.lastModified() < input.lastModified();
    }

    public BooleanSupplier always() {
        return () -> true;
    }

}
