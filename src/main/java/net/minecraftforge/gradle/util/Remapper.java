package net.minecraftforge.gradle.util;

import groovy.lang.Closure;
import net.minecraftforge.gradle.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.io.File;
import java.util.Set;

public class Remapper {

    public static void addDeobfMethod(Project project) {
        DependencyHandler deps = project.getDependencies();
        Util.addMethod(deps, "deobf", new Closure<Object>(deps) {
            public Object doCall(Object param) {
                return resolveDeobf(project, deps.create(param));
            }
        });
    }

    private static Object resolveDeobf(Project project, Dependency dependency) {
        // Create dummy configuration to resolve deps, then delete it
        Configuration cfg = project.getConfigurations().maybeCreate("tmp");
        cfg.getDependencies().add(dependency);
        Set<File> files = cfg.resolve();
        project.getConfigurations().remove(cfg);

        // What *ARE* the deps?
        System.out.println(files);

        return dependency;
    }

}
