package net.minecraftforge.gradle.plugin;

import net.minecraftforge.gradle.api.ForgeGradleAPI;
import net.minecraftforge.gradle.api.mapping.MappingManager;

public class ForgeGradleAPIImpl implements ForgeGradleAPI {

    private final ForgeGradlePluginInstance fg;

    ForgeGradleAPIImpl(ForgeGradlePluginInstance fg) {
        this.fg = fg;
    }

    @Override
    public MappingManager getMappingManager() {
        return fg.mappings;
    }

}
