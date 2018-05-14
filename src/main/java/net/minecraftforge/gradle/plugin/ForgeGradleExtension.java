package net.minecraftforge.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

/**
 * Makes variables available to buildscripts.
 */
public class ForgeGradleExtension {

    public final Minecraft minecraft;
    public final Forge forge;
    public final Mappings mappings;
    public boolean reobfuscateJar = true;

    @Inject
    public ForgeGradleExtension(Project project, ObjectFactory factory) {
        minecraft = factory.newInstance(Minecraft.class);
        forge = factory.newInstance(Forge.class);
        mappings = factory.newInstance(Mappings.class);
    }

    void minecraft(Action<? super Minecraft> action) {
        action.execute(minecraft);
    }

    void forge(Action<? super Forge> action) {
        action.execute(forge);
    }

    void mappings(Action<? super Mappings> action) {
        action.execute(mappings);
    }

    public static class Minecraft {
        public String version;
    }

    public static class Forge {
        public String version;
    }

    public static class Mappings {
        public String version;
    }

}
