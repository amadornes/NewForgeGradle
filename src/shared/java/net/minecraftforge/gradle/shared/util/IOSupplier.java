package net.minecraftforge.gradle.shared.util;

import java.io.IOException;

public interface IOSupplier<T> {

    T get() throws IOException;

}
