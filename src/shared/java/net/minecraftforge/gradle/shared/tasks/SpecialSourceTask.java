package net.minecraftforge.gradle.shared.tasks;

import com.google.common.base.Preconditions;
import net.md_5.specialsource.Jar;
import net.md_5.specialsource.JarRemapper;
import net.minecraftforge.gradle.shared.util.Util;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * Task that takes a jar, applies SpecialSource to it, and outputs another jar.<br/>
 * Supports conditional remapping.
 */
public abstract class SpecialSourceTask extends DefaultTask {

    @InputFile
    protected File input;
    @OutputFile
    protected File output;

    public SpecialSourceTask() {
        getOutputs().upToDateWhen(task -> isUpToDate());
    }

    protected abstract boolean isUpToDate();

    protected void validate() {
    }

    protected abstract JarRemapper createRemapper(Jar inputJar, @Nullable FileCollection classpath) throws IOException;

    @TaskAction
    public final void remap() throws Exception {
        Preconditions.checkNotNull(input, "Input jar is not defined!");
        Preconditions.checkNotNull(output, "Output jar is not defined!");
        validate();

        getProject().delete(output);

        Util.applySpecialSource(input, output, jar -> createRemapper(jar, null));
    }

    public void input(File input) {
        this.input = input;
    }

    public void input(String input) {
        input(getProject().file(input));
    }

    public void output(File output) {
        this.output = output;
    }

    public void output(String output) {
        output(getProject().file(output));
    }

}
