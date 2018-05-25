package net.minecraftforge.gradle.util;

import java.io.IOException;

@FunctionalInterface
public interface IOFunction<A, B> {

    B apply(A arg) throws IOException;

}
