package net.minecraftforge.gradle.groovy

import net.minecraftforge.gradle.api.mapping.MappingProvider
import net.minecraftforge.gradle.mappings.MCPMappingProvider
import net.minecraftforge.gradle.mappings.Remapper
import net.minecraftforge.gradle.plugin.ForgeGradlePluginInstance
import org.gradle.api.Action

import java.util.function.Supplier

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
    }

    private static void extendDependencies(ForgeGradlePluginInstance fg) {
        fg.project.dependencies.metaClass.deobf = { Object target ->
            def dep = fg.project.dependencies.create(target)
            return Remapper.deobfDependency(fg, dep)
        }
        fg.project.dependencies.metaClass.remap = { Map args, Object target ->
            def dep = fg.project.dependencies.create(target)
            return Remapper.remapDependency(fg, dep, args)
        }

        fg.project.dependencies.metaClass.forge = { String version ->
            Supplier depSupplier = {
                return fg.project.dependencies.create("net.minecraftforge:forge:"
                        + fg.fgExt.minecraft.version + "-$version:universal")
            }
            return Remapper.remapDependency(fg, depSupplier, [mapping: fg.fgExt.mappings.forgeMappings])
        }
    }

}