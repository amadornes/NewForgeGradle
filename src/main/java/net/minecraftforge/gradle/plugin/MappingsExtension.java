package net.minecraftforge.gradle.plugin;

import net.minecraftforge.gradle.api.MappingProvider;
import net.minecraftforge.gradle.mappings.MappingManager;

import javax.inject.Inject;

/**
 * Extension that allows users to register their own mapping providers.
 */
public class MappingsExtension {

    private final MappingManager manager;

    @Inject
    public MappingsExtension(MappingManager manager) {
        this.manager = manager;
    }

    public void register(String name, MappingProvider provider) {
        manager.register(name, provider);
    }

}
