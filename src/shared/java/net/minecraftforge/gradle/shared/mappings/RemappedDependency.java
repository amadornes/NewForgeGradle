package net.minecraftforge.gradle.shared.mappings;

import net.minecraftforge.gradle.api.mapping.MappingVersion;
import org.gradle.api.Incubating;
import org.gradle.api.artifacts.Dependency;

import javax.annotation.Nullable;

public class RemappedDependency implements Dependency {

    private final Dependency dependency;
    private final MappingVersion mapping;

    public RemappedDependency(Dependency dependency, MappingVersion mapping) {
        this.dependency = dependency;
        this.mapping = mapping;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public MappingVersion getMapping() {
        return mapping;
    }

    @Override
    @Nullable
    public String getGroup() {
        return dependency.getGroup();
    }

    @Override
    public String getName() {
        return dependency.getName();
    }

    @Override
    @Nullable
    public String getVersion() {
        return dependency.getVersion();
    }

    @Override
    public boolean contentEquals(Dependency dependency) {
        return this.dependency.contentEquals(dependency);
    }

    @Override
    public Dependency copy() {
        return new RemappedDependency(dependency, mapping);
    }

    @Override
    @Incubating
    @Nullable
    public String getReason() {
        return dependency.getReason();
    }

    @Override
    @Incubating
    public void because(@Nullable String s) {
        dependency.because(s);
    }

}
