package net.minecraftforge.gradle.api.mapping;

import java.io.File;

/**
 * Interface to register and access {@link MappingProvider} instances.
 */
public interface MappingManager {

    /**
     * Registers a mapping provider with the specified name.
     */
    void register(MappingProvider provider);

    /**
     * Checks whether a mapping provider with the specified name exists.
     */
    boolean isRegistered(String name);

    /**
     * Gets the mapping file for the specified version or generates it
     * if it does not exist already.
     */
    File getMapping(MappingVersion version);

    /**
     * Generates a hash for the specified mapping version.
     */
    String getMappingHash(MappingVersion version);

}
