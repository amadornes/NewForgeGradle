package net.minecraftforge.gradle;

import groovy.lang.Closure;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.runtime.metaclass.ClosureMetaMethod;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Util {

    /**
     * Converts a file collection into a URL array.
     */
    public static URL[] toURLs(FileCollection files) throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        for (File file : files.getFiles()) {
            urls.add(file.toURI().toURL());
        }
        return urls.toArray(new URL[urls.size()]);
    }

    public static void addMethod(Object target, String name, Closure<?> closure) {
        if (!(target instanceof GroovyObject)) {
            throw new RuntimeException("Cannot add methods to the specified object!");
        }

        MetaClass metaClass = ((GroovyObject) target).getMetaClass();
        ExpandoMetaClass emc;
        if (metaClass instanceof ExpandoMetaClass) {
            emc = (ExpandoMetaClass) metaClass;
        } else {
            emc = new ExpandoMetaClass(target.getClass());
            ((GroovyObject) target).setMetaClass(emc);
        }

        for (Method method : closure.getClass().getDeclaredMethods()) {
            if (!method.getName().equals("doCall")) continue;
            emc.addMetaMethod(new ClosureMetaMethod(name, closure, CachedMethod.find(method)));
        }
    }

}
