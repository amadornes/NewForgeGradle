package net.minecraftforge.gradle.api.mapping;

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

}
