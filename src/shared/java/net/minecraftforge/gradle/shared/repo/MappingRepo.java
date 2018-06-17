package net.minecraftforge.gradle.shared.repo;

import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.mappings.MappingManagerImpl;
import net.minecraftforge.gradle.shared.util.IOSupplier;
import net.minecraftforge.gradle.shared.util.POMBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactIdentifier;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

public class MappingRepo {

    public static CustomRepository add(Project project, MappingManagerImpl manager, String mcVersion, String name, Object url) {
        RepositoryHandler handler = project.getRepositories();
        return CustomRepository.add(handler, name, url, new ArtifactProvider(manager, mcVersion), null);
    }

    private static class ArtifactProvider extends CustomRepository.ArtifactProvider.Simple {

        private final MappingManagerImpl manager;
        private final String mcVersion;

        private ArtifactProvider(MappingManagerImpl manager, String mcVersion) {
            this.manager = manager;
            this.mcVersion = mcVersion;
            addExtensionProvider("srg", this::getMapping);
            addExtensionProvider("pom", this::getPOM);
        }

        private IOSupplier<StreamedResource> getMapping(ArtifactIdentifier identifier) {
            MappingVersion version = MappingVersion.fromMavenArtifactIdentifier(identifier, mcVersion);
            if (version == null) return null;
            byte[] mapping = manager.computeMapping(version);
            if (mapping == null) return null;
            return () -> new StreamedResource.ByteArrayStreamedResource(mapping);
        }

        private IOSupplier<StreamedResource> getPOM(ArtifactIdentifier identifier) {
            String group = identifier.getModuleVersionIdentifier().getGroup();
            String name = identifier.getModuleVersionIdentifier().getName();
            String version = identifier.getModuleVersionIdentifier().getVersion();

            POMBuilder builder = new POMBuilder(group, name, version);
            builder.description(name);

            String pom = builder.tryBuild();
            if (pom == null) return null;
            return () -> new StreamedResource.ByteArrayStreamedResource(pom.getBytes());
        }

    }

}
