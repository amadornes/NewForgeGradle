package net.minecraftforge.gradle.util;

import org.gradle.api.Action;
import org.gradle.api.Transformer;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;
import org.gradle.internal.resource.AbstractExternalResource;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.ExternalResourceReadResult;
import org.gradle.internal.resource.ExternalResourceWriteResult;
import org.gradle.internal.resource.ReadableContent;
import org.gradle.internal.resource.ResourceExceptions;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Object that represents a gradle {@link ExternalResource} that is
 * streamed from a URL, be it directly of through a custom streamer.
 */
public class StreamedExternalResource extends AbstractExternalResource {

    private final URL url;
    private final IOFunction<URL, StreamedResource> streamer;

    public StreamedExternalResource(URL url, IOFunction<URL, StreamedResource> streamer) {
        this.url = url;
        this.streamer = streamer;
    }

    @Override
    public URI getURI() {
        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nullable
    @Override
    public ExternalResourceReadResult<Void> writeToIfPresent(File file) throws ResourceException {
        try {
            StreamedResource res = streamer.apply(url);
            if (res == null) return null;
            InputStream is = res.getStream();
            FileOutputStream fos = new FileOutputStream(file);
            int bytes = IOUtils.copy(is, fos);
            is.close();
            res.close();
            fos.close();
            return ExternalResourceReadResult.of(bytes);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public ExternalResourceReadResult<Void> writeTo(OutputStream outputStream) throws ResourceException {
        try {
            StreamedResource res = streamer.apply(url);
            if (res == null) throw ResourceExceptions.getMissing(getURI());
            InputStream is = res.getStream();
            int bytes = IOUtils.copy(is, outputStream);
            is.close();
            res.close();
            return ExternalResourceReadResult.of(bytes);
        } catch (IOException ex) {
            throw ResourceExceptions.getFailed(getURI(), ex);
        }
    }

    @Override
    public ExternalResourceReadResult<Void> withContent(Action<? super InputStream> action) throws ResourceException {
        try {
            StreamedResource res = streamer.apply(url);
            if (res == null) throw ResourceExceptions.getMissing(getURI());
            InputStream is = res.getStream();
            int initiallyAvailable = is.available();
            action.execute(is);
            int availableNow = is.available();
            is.close();
            res.close();
            return ExternalResourceReadResult.of(initiallyAvailable - availableNow);
        } catch (IOException ex) {
            throw ResourceExceptions.getFailed(getURI(), ex);
        }
    }

    @Nullable
    @Override
    public <T> ExternalResourceReadResult<T> withContentIfPresent(Transformer<? extends T, ? super InputStream> transformer) throws ResourceException {
        try {
            StreamedResource res = streamer.apply(url);
            if (res == null) return null;
            InputStream is = res.getStream();
            int initiallyAvailable = is.available();
            T result = transformer.transform(is);
            int availableNow = is.available();
            is.close();
            res.close();
            return ExternalResourceReadResult.of(initiallyAvailable - availableNow, result);
        } catch (IOException ex) {
            return null;
        }
    }

    @Nullable
    @Override
    public <T> ExternalResourceReadResult<T> withContentIfPresent(ContentAction<? extends T> contentAction) throws ResourceException {
        try {
            StreamedResource res = streamer.apply(url);
            if (res == null) return null;
            InputStream is = res.getStream();
            int initiallyAvailable = is.available();
            T result = contentAction.execute(is, getMetaData());
            int availableNow = is.available();
            is.close();
            res.close();
            return ExternalResourceReadResult.of(initiallyAvailable - availableNow, result);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public ExternalResourceWriteResult put(ReadableContent readableContent) throws ResourceException {
        throw ResourceExceptions.failure(getURI(), "Cannot write to a streamed resource!", null);
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
            StreamedResource res = streamer.apply(url);
            if (res == null) return null;
            return res.getMetadata();
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        return url.toString();
    }

}
