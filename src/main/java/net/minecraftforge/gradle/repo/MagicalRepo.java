package net.minecraftforge.gradle.repo;

import net.minecraftforge.gradle.util.IOFunction;
import net.minecraftforge.gradle.util.StreamedExternalResource;
import net.minecraftforge.gradle.util.StreamedResource;
import net.minecraftforge.gradle.util.Util;
import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository;
import org.gradle.api.internal.artifacts.repositories.AbstractArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceArtifactResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.MavenResolver;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ExternalResourceRepository;
import org.gradle.internal.resource.transfer.CacheAwareExternalResourceAccessor;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A repository forged with unicorn farts and some of the
 * belly button lint from that fat man over there.
 * <p>
 * Allows you to provide anything as a maven resource by
 * creating a regular maven repository and overriding its
 * backing {@link ExternalResourceRepository} with a custom one.
 */
public class MagicalRepo extends AbstractArtifactRepository implements ResolutionAwareRepository {

    public static MagicalRepo add(RepositoryHandler handler, String name, Object url, IOFunction<URL, StreamedResource> streamer) {
        return add(handler, name, url, new StreamingRepo(streamer));
    }

    public static MagicalRepo add(RepositoryHandler handler, String name, Object url, ExternalResourceRepository src) {
        // Create the real maven repo we'll be using and remove it
        MavenArtifactRepository maven = handler.maven($ -> {
            $.setName(name);
            $.setUrl(url);
        });
        handler.remove(maven);

        // Add our own custom repo instead, using the real one in the background
        MagicalRepo repo = new MagicalRepo((DefaultMavenArtifactRepository) maven, src);
        handler.add(repo);
        return repo;
    }

    private final DefaultMavenArtifactRepository maven;
    private final ExternalResourceRepository repo;

    private MagicalRepo(DefaultMavenArtifactRepository maven, ExternalResourceRepository repo) {
        this.maven = maven;
        this.repo = repo;
    }

    @Override
    public String getName() {
        return maven.getName(); // Proxy to the real repo
    }

    @Override
    public void setName(String name) {
        maven.setName(name); // Proxy to the real repo
    }

    @Override
    public String getDisplayName() {
        return maven.getDisplayName(); // Proxy to the real repo
    }

    @Override
    public void onAddToContainer(NamedDomainObjectCollection<ArtifactRepository> container) {
        // No-op. The real repo will get this already
    }

    @Override
    public ConfiguredModuleComponentRepository createResolver() {
        // Create a resolver and get all the fields we need to modify
        MavenResolver resolver = (MavenResolver) maven.createResolver();
        ExternalResourceArtifactResolver artifactResolver = Util.invoke(resolver, ExternalResourceResolver.class, "createArtifactResolver");
        CacheAwareExternalResourceAccessor accessor = Util.get(artifactResolver, artifactResolver.getClass(), "resourceAccessor");
        // Inject our custom repository into the resolver
        Util.setFinal(resolver, ExternalResourceResolver.class, "repository", repo);
        Util.setFinal(accessor, accessor.getClass(), "delegate", repo);
        return resolver;
    }

    /**
     * A simple {@link ExternalResourceRepository} implementation that
     * gets resources from a custom {@link URL}->{@link StreamedResource} mapper.
     */
    private static class StreamingRepo implements ExternalResourceRepository {

        private final IOFunction<URL, StreamedResource> streamer;

        private StreamingRepo(IOFunction<URL, StreamedResource> streamer) {
            this.streamer = streamer;
        }

        @Override
        public ExternalResourceRepository withProgressLogging() {
            return this;
        }

        @Override
        public ExternalResource resource(ExternalResourceName name, boolean revalidate) {
            try {
                return new StreamedExternalResource(name.getUri().toURL(), streamer);
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public ExternalResource resource(ExternalResourceName name) {
            return resource(name, false);
        }

    }

}
