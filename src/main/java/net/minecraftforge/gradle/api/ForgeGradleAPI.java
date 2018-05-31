package net.minecraftforge.gradle.api;

import net.minecraftforge.gradle.api.mapping.MappingManager;

/**
 * Main ForgeGradle API interface.
 */
public interface ForgeGradleAPI {

    /**
     * Gets the {@link MappingManager} for this instance of ForgeGradle.
     */
    MappingManager getMappingManager();

}
