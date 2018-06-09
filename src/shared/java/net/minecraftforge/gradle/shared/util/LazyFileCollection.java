package net.minecraftforge.gradle.shared.util;

import org.gradle.api.internal.file.AbstractFileCollection;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Set;
import java.util.function.Supplier;

public class LazyFileCollection extends AbstractFileCollection {

    private final String name;
    private final Supplier<Set<File>> supplier;
    private Set<File> files = null;

    public LazyFileCollection(String name, Supplier<Set<File>> supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return name;
    }

    @Override
    @Nonnull
    public Set<File> getFiles() {
        return files == null ? files = supplier.get() : files;
    }

}
