package net.minecraftforge.gradle.shared.repo;

import net.minecraftforge.gradle.api.mapping.MappingVersion;
import net.minecraftforge.gradle.shared.mappings.MappingManagerImpl;
import net.minecraftforge.gradle.shared.util.IOSupplier;
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
        }

        @Override
        public boolean supportsPOMs() {
            return false;
        }

        private IOSupplier<StreamedResource> getMapping(ArtifactIdentifier identifier) {
            MappingVersion mappingVersion = MappingVersion.fromMavenArtifactIdentifier(identifier, mcVersion);
            if (mappingVersion == null) return null;
            byte[] mapping = manager.computeMapping(mappingVersion);
            if (mapping == null) return null;
            return () -> StreamedResource.ofBytes(mapping);
        }

    }

}
