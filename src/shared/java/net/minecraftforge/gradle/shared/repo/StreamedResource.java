package net.minecraftforge.gradle.shared.repo;

import org.gradle.internal.hash.HashUtil;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

/**
 * Interface that represents a resource which can be streamed.<br/>
 * This resource has a single input stream and <b>must</b> be closed
 * once it is not needed anymore.
 */
public interface StreamedResource extends Closeable {

    InputStream getStream() throws IOException;

    ExternalResourceMetaData getMetadata(URI uri);

    @Override
    void close() throws IOException;

    class URLStreamedResource implements StreamedResource {

        private final URLConnection connection;
        private final long length;
        @Nullable
        private final Date date;
        @Nullable
        private final HashValue hash;

        public URLStreamedResource(URL url) throws IOException {
            this(url, -1, null, null);
        }

        public URLStreamedResource(URL url, long length, @Nullable Date date, @Nullable HashValue hash) throws IOException {
            this.connection = url.openConnection();
            this.hash = hash;
            this.length = length != -1 ? length : this.connection.getContentLengthLong();
            this.date = date != null ? date : new Date(connection.getDate());
        }

        @Override
        public InputStream getStream() throws IOException {
            return connection.getInputStream();
        }

        @Override
        public ExternalResourceMetaData getMetadata(URI uri) {
            return new DefaultExternalResourceMetaData(uri, date, length, null, null, hash);
        }

        @Override
        public void close() throws IOException {
            getStream().close();
        }

    }

    class ByteArrayStreamedResource implements StreamedResource {

        private final byte[] bytes;
        private InputStream stream;

        public ByteArrayStreamedResource(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public InputStream getStream() {
            if (stream == null) stream = new ByteArrayInputStream(bytes);
            return stream;
        }

        @Override
        public ExternalResourceMetaData getMetadata(URI uri) {
            return new DefaultExternalResourceMetaData(uri, new Date().getTime(), bytes.length,
                    null, null, HashUtil.sha1(bytes));
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

    }

}
