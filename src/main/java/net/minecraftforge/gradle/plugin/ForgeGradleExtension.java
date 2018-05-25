package net.minecraftforge.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;
import java.util.Map;

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

    void mappings(Map<String, String> map) {
        if (map.containsKey("provider")) mappings.provider = map.get("provider");
        if (map.containsKey("channel")) mappings.channel = map.get("channel");
        if (map.containsKey("version")) mappings.version = map.get("version");
        if (map.containsKey("mcMappings")) mappings.mcMappings = map.get("mcMappings");
        if (map.containsKey("forgeMappings")) mappings.forgeMappings = map.get("forgeMappings");
        if (map.containsKey("deobfMappings")) mappings.deobfMappings = map.get("deobfMappings");
        if (map.containsKey("obfMappings")) mappings.obfMappings = map.get("obfMappings");
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
        public String provider = "mcp";
        public String channel;
        public String version;

        public String mcMappings = "notch-mcp";
        public String forgeMappings = "notch-mcp";
        public String deobfMappings = "srg-mcp";
        public String obfMappings = "mcp-srg";
    }

    public static class Builtin {

        public boolean mcpMappings = true;

        public void disableAll() {
            mcpMappings = false;
        }

    }

}
