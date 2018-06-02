package net.minecraftforge.gradle.util;

import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
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

    ExternalResourceMetaData getMetadata();

    @Override
    void close() throws IOException;

    class URLStreamedResource implements StreamedResource {

        private final URL url;
        private final URLConnection connection;
        private final int length;
        private final long date;

        public URLStreamedResource(URL url) throws IOException {
            this.url = url;
            this.connection = url.openConnection();
            this.length = this.connection.getContentLength();
            this.date = connection.getDate();
        }

        @Override
        public InputStream getStream() throws IOException {
            return connection.getInputStream();
        }

        @Override
        public ExternalResourceMetaData getMetadata() {
            return new DefaultExternalResourceMetaData(Util.getURI(url), date, length);
        }

        @Override
        public void close() throws IOException {
            getStream().close();
        }

    }

    class ByteArrayStreamedResource implements StreamedResource {

        private final URL url;
        private final byte[] bytes;
        private InputStream stream;

        public ByteArrayStreamedResource(URL url, byte[] bytes) {
            this.url = url;
            this.bytes = bytes;
        }

        @Override
        public InputStream getStream() {
            if (stream == null) stream = new ByteArrayInputStream(bytes);
            return stream;
        }

        @Override
        public ExternalResourceMetaData getMetadata() {
            return new DefaultExternalResourceMetaData(Util.getURI(url), new Date().getTime(), bytes.length);
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

    }

}
