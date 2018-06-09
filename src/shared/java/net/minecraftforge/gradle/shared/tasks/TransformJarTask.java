package net.minecraftforge.gradle.shared.tasks;

import com.google.common.collect.Iterables;
import groovy.lang.Closure;
import net.md_5.specialsource.Jar;
import net.md_5.specialsource.JarRemapper;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.function.BooleanSupplier;

/**
 * Task that takes a jar, applies a set of transformers to it, and outputs another jar.<br/>
 * Supports conditional transformation.
 */
public class TransformJarTask extends SpecialSourceTask {

    @Input
    private Iterable<Transformer> transformers;

    @Input
    private BooleanSupplier condition = () -> true;

    public TransformJarTask() {
        this.transformers = Collections.emptyList();
    }

    @Override
    protected boolean isUpToDate() {
        if (Iterables.isEmpty(transformers)) {
            try {
                return FileUtils.contentEquals(input, output);
            } catch (IOException e) {
                return false;
            }
        }
        return !condition.getAsBoolean();
    }

    @Override
    protected JarRemapper createRemapper(Jar inputJar, @Nullable FileCollection classpath) {
        return null; // TODO: Create remapper
    }

    public void addTransformers(Iterable<Transformer> transformers) {
        this.transformers = Iterables.concat(this.transformers, transformers);
    }

    public void addTransformer(Transformer transformer) {
        addTransformers(Collections.singleton(transformer));
    }

    public void addTransformer(Closure<byte[]> transformer) {
        addTransformer(transformer::call);
    }

    public void transformWhen(BooleanSupplier condition) {
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

    public interface Transformer {

        byte[] transform(byte[] bytes);

    }

}
