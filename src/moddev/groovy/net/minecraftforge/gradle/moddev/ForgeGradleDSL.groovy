package net.minecraftforge.gradle.moddev

import net.minecraftforge.gradle.api.mapping.MappingProvider
import net.minecraftforge.gradle.api.mapping.MappingVersion
import net.minecraftforge.gradle.shared.impl.MCLauncherArtifactProvider
import net.minecraftforge.gradle.shared.impl.MCPMappingProvider
import net.minecraftforge.gradle.shared.mappings.RemappedDependency
import net.minecraftforge.gradle.shared.repo.CustomRepository
import org.gradle.api.Action

class ForgeGradleDSL {

    static void extendDSL(ForgeGradlePluginInstance fg) {
        extendRepositories(fg)
        extendDependencies(fg)
    }

    private static void extendRepositories(ForgeGradlePluginInstance fg) {
        fg.project.repositories.metaClass.mappings = { MappingProvider provider ->
            fg.mappings.register(provider)
            return provider
        }
        fg.project.repositories.metaClass.mappings = { MappingProvider provider, Action action ->
            action.execute(provider)
            fg.mappings.register(provider)
            return provider
        }

        fg.project.repositories.metaClass.mcp = { ->
            return fg.project.objects.newInstance(MCPMappingProvider.class)
        }

        fg.project.repositories.metaClass.forgeMaven = {
            return fg.project.repositories.maven {
                it.name = "forge"
                it.url = "http://files.minecraftforge.net/maven"
            }
        }

        fg.project.repositories.metaClass.minecraftMaven = {
            CustomRepository.add(fg.project.repositories, "mclauncher", "https://launcher.mojang.com/", new MCLauncherArtifactProvider(), null)
            fg.project.repositories.maven {
                it.name = "mclibraries"
                it.url = "https://libraries.minecraft.net"
                it.metadataSources {
                    it.artifact() // Don't even start looking for these guys' deps... It ends in hell
                }
            }
        }
    }

    private static void extendDependencies(ForgeGradlePluginInstance fg) {
        fg.project.dependencies.metaClass.deobf = { Object target ->
            def dep = fg.project.dependencies.create(target)
            return new RemappedDependency(dep, MappingVersion.lazy({
                return new MappingVersion(fg.fgExt.mappings.provider, fg.fgExt.mappings.channel,
                        fg.fgExt.mappings.version, fg.fgExt.minecraft.version, fg.fgExt.mappings.deobfMappings)
            }))
        }
        fg.project.dependencies.metaClass.remap = { Map args, Object target ->
            def dep = fg.project.dependencies.create(target)
            return new RemappedDependency(dep, MappingVersion.lazy({
                String provider = args.getOrDefault('provider', fg.fgExt.mappings.provider)
                String channel = args.getOrDefault('channel', fg.fgExt.mappings.channel)
                String version = args.getOrDefault('version', fg.fgExt.mappings.version)
                String mapping = args.get('mapping')
                return new MappingVersion(provider, channel, version, fg.fgExt.minecraft.version, mapping)
            }))
        }
        fg.project.dependencies.metaClass.forge = { String version ->
            fg.project.dependencies.remap("net.minecraftforge:forge:${fg.fgExt.minecraft.version}-${version}:universal", mapping: 'notch-mcp')
        }
        fg.project.dependencies.metaClass.minecraftClient = { String version ->
            fg.project.dependencies.remap("net.minecraft:client:${fg.fgExt.minecraft.version}", mapping: 'notch-mcp')
        }
        fg.project.dependencies.metaClass.minecraftServer = { String version ->
            fg.project.dependencies.remap("net.minecraft:server-pure:${fg.fgExt.minecraft.version}", mapping: 'notch-mcp')
        }
    }

}