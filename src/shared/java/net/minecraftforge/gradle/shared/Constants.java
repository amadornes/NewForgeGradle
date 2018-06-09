package net.minecraftforge.gradle.shared;

public final class Constants {

    public static final String PLUGIN_NAME = "ForgeGradle";
    public static final String PLUGIN_API_PROPERTY_NAME = "forgegradle_api";

    public static final String FORGE_GRADLE_EXTENSION_NAME = "forgegradle";
    public static final String MAPPINGS_EXTENSION_NAME = "mappings";

    public static final String TASK_BUILD = "build";
    public static final String TASK_REOBFUSCATE = "reobf";

    public static final String CACHE_BASE_DIR = "caches/minecraft/";
    public static final String CACHE_GENERATED_DIR = CACHE_BASE_DIR + "generated/";
    public static final String CACHE_GENERATED_MAPPINGS_DIR = CACHE_GENERATED_DIR + "mappings/";

    public static final String MAVEN_FORGE = "http://files.minecraftforge.net/maven/";

}
