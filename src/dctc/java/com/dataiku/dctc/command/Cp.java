package com.dataiku.dctc.command;

import static com.dataiku.dip.utils.PrettyString.scat;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.Options;

import com.dataiku.dctc.GlobalConstants;
import com.dataiku.dctc.command.abs.ListFilesCommand;
import com.dataiku.dctc.configuration.GlobalConf;
import com.dataiku.dctc.copy.CopyTask;
import com.dataiku.dctc.copy.CopyTasksExecutor;
import com.dataiku.dctc.copy.SimpleCopyTaskRunnableFactory;
import com.dataiku.dctc.display.Interactive;
import com.dataiku.dctc.display.ThreadedDisplay;
import com.dataiku.dctc.file.GeneralizedFile;
import com.dataiku.dip.utils.IndentedWriter;

public class Cp extends ListFilesCommand {
    public String tagline() {
        return "Copy files and directories.";
    }
    public void longDescription(IndentedWriter printer) {
        printer.paragraph(scat("Copy all sources to the target. Target is create as"
                               ," a folder if the are several sources or the source is a folder.")
                          ,scat("Dctc cp can compress or uncompress GZip-compressed inputs and outputs."
                                ,"Dctc cp can also be used to create or expand Zip archives."));
    }

    // Public
    @Override
    public final String cmdname() {
        return "cp";
    }
    @Override
    protected String proto() {
        return "[OPT...] SOURCE... DST";
    }
    /// Getters
    public boolean noClobber() {
        return hasOption("n");
    }
    public boolean recur() {
        if (recur == null) {
            recur = hasOption("r") || hasOption("R");
        }
        return recur;
    }
    public boolean interactive() {
        if (interactive == null) {
            interactive = hasOption("i");
        }
        return interactive;
    }

    // Protected
    @Override
    protected Options setOptions() {
        Options opt = new Options();
        opt.addOption("R", false, "Copy directories recursively.");
        opt.addOption("r", "recursive", false, "Copy directories recursively.");
        opt.addOption("c", "compress", false, "Compress all input files and add .gz extension.");
        opt.addOption("u", "uncompress", false, "Uncompress all compressed (ie, .gz) input files (strips .gz extension)");
        opt.addOption(GlobalConstants.UNARCHIVE_OPT, "unarchive", false, "Uncompress all archives found in the command line.");
        opt.addOption("i", "interactive", false, "Prompt before overwrite");
        opt.addOption("a", "archive", false, "Archives all input files into a single destination file using the destination file extension as identifier for the archive method. Supported archive methods are 'zip'");
        opt.addOption("p", "preserve", false, "Preserve the time stamp");
        opt.addOption("s", "sequential", false, "Make the copy with only one thread.");
        longOpt(opt, "Set the number of thread.", "thread_number", "n", "number");

        return opt;
    }
    @Override
    public void execute(List<CopyTask> tasks) {
        SimpleCopyTaskRunnableFactory fact = new SimpleCopyTaskRunnableFactory(unarchive(),
                                                                               archive(),
                                                                               hasOption("p"));
        ThreadedDisplay display = GlobalConf.getDisplay();
        CopyTasksExecutor exec = new CopyTasksExecutor(fact, display, getThreadLimit());
        try {
            exec.run(tasks, archive());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (exec.hasFail()) {
            setExitCode(2);
            exec.displayErrors();
        }
    }
    @Override
    protected boolean recursion() {
        return recur();
    }
    @Override
    protected boolean includeLastPathElementInTarget() {
        return !archive();
    }
    @Override
    protected boolean shouldAdd(GeneralizedFile src, GeneralizedFile dst, String root) {
        try {
            return !dst.exists() || noClobber() || ask(dst);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
    @Override
    protected void dstRoot(GeneralizedFile dst) {
    }
    @Override
    protected void leave(GeneralizedFile sourceDir) {
        // empty
    }
    @Override
    protected boolean deleteSource() {
        return false;
    }

    // Private
    private boolean ask(GeneralizedFile file) {
        return !interactive()
                || Interactive.ask("cp", "cp: overwrite `" + file.givenName()
                        + "'?", "yY", "nN");
    }

    // Attributes
    private Boolean recur = null;
    private Boolean interactive = null;
}
