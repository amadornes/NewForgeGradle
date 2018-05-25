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
    public final Builtin builtin;

    @Inject
    public ForgeGradleExtension(Project project) {
        ObjectFactory factory = project.getObjects();
        minecraft = factory.newInstance(Minecraft.class);
        forge = factory.newInstance(Forge.class);
        mappings = factory.newInstance(Mappings.class);
        builtin = factory.newInstance(Builtin.class);
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

    void builtin(Action<? super Builtin> action) {
        action.execute(builtin);
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

    public static class Builtin {

        public boolean mcpMappings = true;

        public void disableAll() {
            mcpMappings = false;
        }

    }

}
