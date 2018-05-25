package net.minecraftforge.gradle.util;

import au.com.bytecode.opencsv.CSVReader;
import groovy.lang.Closure;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import net.md_5.specialsource.Jar;
import net.md_5.specialsource.JarRemapper;
import net.minecraftforge.gradle.plugin.ForgeGradlePluginInstance;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.runtime.metaclass.ClosureMetaMethod;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;
import org.gradle.internal.impldep.org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    /**
     * Adds the specified closure as a method in the target.<br/>
     * Methods inside the closure must be named {@code doCall} and
     * return the generic type of the closure.
     */
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

    /**
     * Resolves a dependency, downloading the file and its transitives
     * if not cached and returns the set of files.
     */
    public static Set<File> resolveDependency(ForgeGradlePluginInstance fg, Dependency dependency) {
        Configuration cfg = fg.project.getConfigurations().maybeCreate("resolve_dep_" + fg.dependencyID);
        fg.dependencyID++;
        cfg.getDependencies().add(dependency);
        Set<File> files = cfg.resolve();
        fg.project.getConfigurations().remove(cfg);
        fg.dependencyID--;
        return files;
    }

    /**
     * Resolves a dependency, downloading the file and its transitives
     * if not cached and returns the set of files.
     */
    public static Set<File> resolveDependency(ForgeGradlePluginInstance fg, Object dependency) {
        return resolveDependency(fg, fg.project.getDependencies().create(dependency));
    }

    /**
     * Unzips a file into the target directory.
     */
    public static void unzip(File file, File targetDir) throws IOException {
        ZipFile zip = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File out = new File(targetDir, entry.getName());
            if (entry.isDirectory()) {
                out.mkdirs();
            } else {
                out.getParentFile().mkdirs();
                InputStream inputStream = zip.getInputStream(entry);
                OutputStream outputStream = new FileOutputStream(out);
                IOUtils.copy(inputStream, outputStream);
                IOUtils.closeQuietly(inputStream);
                outputStream.close();
            }
        }
        zip.close();
    }

    /**
     * Unzips a file to a folder named {@code extracted} in the same directory.
     */
    public static File unzip(File file) {
        File target = new File(file.getParentFile(), "extracted");
        if (target.exists()) return target;

        try {
            Util.unzip(file, target);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return target;
    }

    /**
     * Reads a CSV file and returns a map of the two requested columns.
     */
    public static Map<String, String> readCSV(File file, String column1, String column2) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(file));

        Map<String, String> mappings = new HashMap<>();

        String[] header = reader.readNext();
        int pos1 = ArrayUtils.indexOf(header, column1);
        int pos2 = ArrayUtils.indexOf(header, column2);

        for (String[] entry : reader.readAll()) {
            mappings.put(entry[pos1], entry[pos2]);
        }

        reader.close();

        return mappings;
    }

    public static void applySpecialSource(File input, File output, IOFunction<Jar, JarRemapper> remapperSupplier) throws IOException {
        Jar inputJar = Jar.init(input);
        remapperSupplier.apply(inputJar).remapJar(inputJar, output);
    }

}
