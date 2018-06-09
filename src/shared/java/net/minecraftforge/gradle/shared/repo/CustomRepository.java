package net.minecraftforge.gradle.shared.repo;

import com.google.common.io.CountingInputStream;
import net.minecraftforge.gradle.shared.util.StreamedResource;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.ArtifactIdentifier;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.DefaultArtifactIdentifier;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository;
import org.gradle.api.internal.artifacts.repositories.AbstractArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceArtifactResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.MavenResolver;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;
import org.gradle.internal.resource.AbstractExternalResource;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ExternalResourceReadResult;
import org.gradle.internal.resource.ExternalResourceRepository;
import org.gradle.internal.resource.ExternalResourceWriteResult;
import org.gradle.internal.resource.ReadableContent;
import org.gradle.internal.resource.ResourceExceptions;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.CacheAwareExternalResourceAccessor;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomRepository extends AbstractArtifactRepository implements ResolutionAwareRepository {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^/(?<group>\\S+(?:/\\S+)*)/(?<name>\\S+)/(?<version>\\S+)/" +
                    "\\2-\\3(?:-(?<classifier>[^.\\s]+))?\\.(?<extension>\\S+)$");

    public static CustomRepository add(RepositoryHandler handler, String name, Object url,
                                       @Nullable ArtifactProvider provider, @Nullable ArtifactStore store) {
        // Create the real maven repo we'll be using and remove it
        MavenArtifactRepository maven = handler.maven($ -> {
            $.setName(name);
            $.setUrl(url);
        });
        handler.remove(maven);

        // Add our own custom repo instead, using the real one in the background
        CustomRepository repo = new CustomRepository((DefaultMavenArtifactRepository) maven, provider, store);
        handler.add(repo);
        return repo;
    }

    private final DefaultMavenArtifactRepository maven;
    @Nullable
    private final ArtifactProvider provider;
    @Nullable
    private final ArtifactStore store;

    private CustomRepository(DefaultMavenArtifactRepository maven, @Nullable ArtifactProvider provider, @Nullable ArtifactStore store) {
        this.maven = maven;
        this.provider = provider;
        this.store = store;
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
        // Create a resolver
        MavenResolver resolver = (MavenResolver) maven.createResolver();
        if (provider != null) {
            // Create a repo
            ExternalResourceRepository repo = new StreamingRepo();
            // Get the fields we want to modify
            ExternalResourceArtifactResolver artifactResolver = Util.invoke(resolver, ExternalResourceResolver.class, "createArtifactResolver");
            CacheAwareExternalResourceAccessor accessor = Util.get(artifactResolver, artifactResolver.getClass(), "resourceAccessor");
            // Inject our custom repository into the resolver
            Util.setFinal(resolver, ExternalResourceResolver.class, "repository", repo);
            Util.setFinal(accessor, accessor.getClass(), "delegate", repo);
        }
        return resolver;
    }

    public interface ArtifactProvider {

        StreamedResource getArtifact(ArtifactIdentifier identifier) throws IOException;

    }

    public interface ArtifactStore {

        File getArtifactPath(File cachesRoot, ArtifactIdentifier identifier, HashValue sha1);

    }

    private class StreamingRepo implements ExternalResourceRepository {

        @Override
        public ExternalResourceRepository withProgressLogging() {
            return this;
        }

        @Override
        public ExternalResource resource(ExternalResourceName name, boolean revalidate) {
            try {
                URI uri = name.getUri();
                URL url = uri.toURL();
                Matcher matcher = URL_PATTERN.matcher(url.getPath());
                if (!matcher.matches()) return null;
                ArtifactIdentifier identifier = new DefaultArtifactIdentifier(
                        new DefaultModuleVersionIdentifier(
                                matcher.group("group").replace('/', '.'),
                                matcher.group("name"),
                                matcher.group("version")
                        ),
                        matcher.group("name"),
                        matcher.group("extension"),
                        matcher.group("extension"),
                        matcher.group("classifier"));
                return new CustomArtifactExternalResource(uri, identifier);
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public ExternalResource resource(ExternalResourceName name) {
            return resource(name, false);
        }

    }

    private class CustomArtifactExternalResource extends AbstractExternalResource {

        private final URI uri;
        private final ArtifactIdentifier identifier;

        private CustomArtifactExternalResource(URI uri, ArtifactIdentifier identifier) {
            this.uri = uri;
            this.identifier = identifier;
        }

        @Override
        public String getDisplayName() {
            return uri.toString();
        }

        @Override
        public URI getURI() {
            return uri;
        }

        private StreamedResource getResource() throws IOException {
            return provider.getArtifact(identifier);
        }

        @Nullable
        @Override
        public ExternalResourceReadResult<Void> writeToIfPresent(File file) throws ResourceException {
            try {
                StreamedResource resource = getResource();
                if (resource == null) return null;
                FileOutputStream out = new FileOutputStream(file);
                ExternalResourceReadResult<Void> result = writeTo(out);
                out.close();
                return result;
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public ExternalResourceReadResult<Void> writeTo(OutputStream out) throws ResourceException {
            return withContent(in -> {
                try {
                    IOUtils.copy(in, out);
                } catch (IOException ex) {
                    throw ResourceExceptions.failure(uri, "Failed to write resource!", ex);
                }
            });
        }

        @Override
        public ExternalResourceReadResult<Void> withContent(Action<? super InputStream> action) throws ResourceException {
            try {
                StreamedResource resource = getResource();
                if (resource == null) throw ResourceExceptions.getMissing(uri);
                CountingInputStream in = new CountingInputStream(resource.getStream());
                action.execute(in);
                in.close();
                return ExternalResourceReadResult.of(in.getCount());
            } catch (IOException ex) {
                throw ResourceExceptions.failure(uri, "Failed to write resource!", ex);
            }
        }

        @Nullable
        @Override
        public <T> ExternalResourceReadResult<T> withContentIfPresent(Transformer<? extends T, ? super InputStream> transformer) throws ResourceException {
            try {
                StreamedResource resource = getResource();
                if (resource == null) return null;
                CountingInputStream in = new CountingInputStream(resource.getStream());
                T result = transformer.transform(in);
                in.close();
                return ExternalResourceReadResult.of(in.getCount(), result);
            } catch (IOException ex) {
                return null;
            }
        }

        @Nullable
        @Override
        public <T> ExternalResourceReadResult<T> withContentIfPresent(ContentAction<? extends T> contentAction) throws ResourceException {
            try {
                StreamedResource resource = getResource();
                if (resource == null) return null;
                CountingInputStream in = new CountingInputStream(resource.getStream());
                T result = contentAction.execute(in, getMetaData());
                in.close();
                return ExternalResourceReadResult.of(in.getCount(), result);
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public ExternalResourceWriteResult put(ReadableContent readableContent) throws ResourceException {
            throw ResourceExceptions.putFailed(uri, null);
        }

        @Nullable
        @Override
        public List<String> list() throws ResourceException {
            return null;
        }

        @Nullable
        @Override
        public ExternalResourceMetaData getMetaData() {
            try {
                StreamedResource resource = getResource();
                if (resource == null) return null;
                return resource.getMetadata(uri);
            } catch (IOException e) {
                return null;
            }
        }

    }

}
