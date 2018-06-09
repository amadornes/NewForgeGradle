package net.minecraftforge.gradle.shared.groovy

import net.minecraftforge.gradle.moddev.ForgeGradlePluginInstance
import net.minecraftforge.gradle.shared.mappings.Remapper

class ForgeGradleDSL {

    static void extendDSL(ForgeGradlePluginInstance fg) {
        extendRepositories(fg)
        extendDependencies(fg)
    }

    private static void extendRepositories(ForgeGradlePluginInstance fg) {
//        fg.project.repositories.metaClass.mappings = { MappingProvider provider ->
//            fg.mappings.register(provider)
//            return provider
//        }
//        fg.project.repositories.metaClass.mappings = { MappingProvider provider, Action action ->
//            action.execute(provider)
//            fg.mappings.register(provider)
//            return provider
//        }
//
//        fg.project.repositories.metaClass.mcp = { ->
//            return fg.project.objects.newInstance(MCPMappingProvider.class)
//        }
//
//        fg.project.repositories.metaClass.forgeMaven = {
//            return fg.project.repositories.maven {
//                it.name = "forge"
//                it.url = "http://files.minecraftforge.net/maven"
//            }
//        }
//
//        fg.project.repositories.metaClass.minecraftMaven = {
//            CustomRepository.add(fg.project.repositories, "mclauncher", "https://launcher.mojang.com/", new MCLauncherArtifactProvider(), null)
//            fg.project.repositories.maven {
//                it.name = "mclibraries"
//                it.url = "https://libraries.minecraft.net"
//                it.metadataSources {
//                    it.artifact() // Don't even start looking for these guys' deps... It ends in hell
//                }
//            }
//        }
    }

    private static void extendDependencies(ForgeGradlePluginInstance fg) {
        fg.project.dependencies.metaClass.deobf = { Object target ->
            def dep = fg.project.dependencies.create(target)
            return Remapper.deobfDependency(fg, dep)
        }
        fg.project.dependencies.metaClass.remap = { Map args, Object target ->
            def dep = fg.project.dependencies.create(target)
            return Remapper.remapDependencyWithDefaults(fg, dep, args)
        }

//        fg.project.dependencies.metaClass.forge = { String version ->
//            Supplier depSupplier = {
//                return fg.project.dependencies.create("net.minecraftforge:forge:"
//                        + fg.fgExt.minecraft.version + "-$version:universal")
//            }
//            return Remapper.remapDependencyWithDefaults(fg, depSupplier, [mapping: fg.fgExt.mappings.forgeMappings])
//        }
    }

}