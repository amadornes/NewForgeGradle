package net.minecraftforge.gradle.tasks;

import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.Iterator;

/**
 * Task that takes a jar, applies a set of access transformers to it, and outputs another jar.<br/>
 * Supports conditional transformation.
 */
public class AccessTransformJarTask extends TransformJarTask {

    public void transformers(FileCollection files) {
        addTransformers(() -> {
            Iterator<File> it = files.iterator();
            return new Iterator<Transformer>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Transformer next() {
                    return new AccessTransformer(it.next());
                }
            };
        });
    }

    public void transformers(File... files) {
        transformers(getProject().files((Object[]) files));
    }

    public void transformers(String... files) {
        transformers(getProject().files((Object[]) files));
    }

    private static class AccessTransformer implements Transformer {

        private final File source;

        private AccessTransformer(File source) {
            this.source = source;
        }

        @Override
        public byte[] transform(byte[] bytes) {
            return bytes;
        }

    }

}
