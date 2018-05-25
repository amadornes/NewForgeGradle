package net.minecraftforge.gradle.plugin;

import net.minecraftforge.gradle.api.MappingProvider;

import javax.inject.Inject;
import java.util.Map;

/**
 * Extension that allows users to register their own mapping providers.
 */
public class MappingsExtension {

    private final Map<String, MappingProvider> providers;

    @Inject
    public MappingsExtension(Map<String, MappingProvider> providers) {
        this.providers = providers;
    }

    public void add(String name, MappingProvider provider) {
        providers.put(name, provider);
    }

}
