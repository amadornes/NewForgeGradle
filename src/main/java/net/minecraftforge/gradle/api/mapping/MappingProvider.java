package net.minecraftforge.gradle.api.mapping;

import org.gradle.api.artifacts.dsl.RepositoryHandler;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Interface that represents a provider of jar mappings.
 */
public interface MappingProvider {

    /**
     * Gets the name of this mapping provider.
     * <p>
     * Will be read on registration and never again.
     */
    String getName();

    /**
     * Adds the repositories required to pull any dependencies this mapping provider may need.
     */
    default void addDependencyRepositories(RepositoryHandler handler) {
    }

    /**
     * Gets the set of mapping names that this provider supports.
     */
    Set<String> getSupportedMappings();

    /**
     * Generates a map of entries for the specified mapping version.
     */
    Map<MappingEntry, MappingEntry> getMapping(MappingVersion version, Function<Object, File> dependencyResolver);

    /**
     * Gets a key->dependency map for the specified mapping version.
     */
    default Map<Object, Object> getDependencies(MappingVersion version) {
        return Collections.emptyMap();
    }

}
