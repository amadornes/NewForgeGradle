package net.minecraftforge.gradle.moddev;

import net.minecraftforge.gradle.api.mapping.MappingManager;
import net.minecraftforge.gradle.api.moddev.ForgeGradleAPI;

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
