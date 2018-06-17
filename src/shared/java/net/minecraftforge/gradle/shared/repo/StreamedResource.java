package net.minecraftforge.gradle.shared.repo;

import net.minecraftforge.gradle.shared.util.IOSupplier;
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
import java.util.function.Supplier;

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
        private final HashValue hash;

        public URLStreamedResource(URL url) throws IOException {
            this(url, -1, null);
        }

        public URLStreamedResource(URL url, long length, @Nullable HashValue hash) throws IOException {
            this.connection = url.openConnection();
            this.hash = hash;
            this.length = length != -1 ? length : this.connection.getContentLengthLong();
        }

        @Override
        public InputStream getStream() throws IOException {
            return connection.getInputStream();
        }

        @Override
        public ExternalResourceMetaData getMetadata(URI uri) {
            return new DefaultExternalResourceMetaData(uri, null, length, null, null, hash);
        }

        @Override
        public void close() throws IOException {
            getStream().close();
        }

    }

    class ByteArrayStreamedResource implements StreamedResource {

        private final Supplier<byte[]> byteSupplier;
        private HashValue hash;
        private InputStream stream;

        public ByteArrayStreamedResource(byte[] bytes) {
            this(() -> bytes);
        }

        public ByteArrayStreamedResource(IOSupplier<byte[]> byteSupplier) {
            this.byteSupplier = new Supplier<byte[]>() {
                private byte[] bytes = null;

                @Override
                public byte[] get() {
                    if (bytes == null) {
                        try {
                            return bytes = byteSupplier.get();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    return bytes;
                }
            };
        }

        public ByteArrayStreamedResource withHash(HashValue hash) {
            this.hash = hash;
            return this;
        }

        private byte[] getBytes() {
            return byteSupplier.get();
        }

        @Override
        public InputStream getStream() {
            if (stream == null) stream = new ByteArrayInputStream(getBytes());
            return stream;
        }

        @Override
        public ExternalResourceMetaData getMetadata(URI uri) {
            return new DefaultExternalResourceMetaData(uri, 0, 0) {
                @Override
                public long getContentLength() {
                    return getBytes().length; // Handle lazily, we may only care about the hash
                }

                @Nullable
                @Override
                public HashValue getSha1() {
                    return hash != null ? hash : HashUtil.sha1(getBytes());
                }
            };
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

    }

}
