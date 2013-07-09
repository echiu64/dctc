package com.dataiku.dctc.command.abs;

import static com.dataiku.dip.utils.PrettyString.pquoted;
import static com.dataiku.dip.utils.PrettyString.scat;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.dataiku.dctc.Globbing;
import com.dataiku.dctc.Main;
import com.dataiku.dctc.clo.LongOption;
import com.dataiku.dctc.clo.Option;
import com.dataiku.dctc.clo.Parser;
import com.dataiku.dctc.clo.ShortOption;
import com.dataiku.dctc.clo.Usage;
import com.dataiku.dctc.command.policy.YellPolicy;
import com.dataiku.dctc.command.policy.YellPolicyFactory;
import com.dataiku.dctc.configuration.GlobalConf;
import com.dataiku.dctc.file.FileBuilder;
import com.dataiku.dctc.file.GeneralizedFile;
import com.dataiku.dctc.utils.ExitCode;
import com.dataiku.dip.utils.IndentedWriter;
public abstract class Command {
    public Command() {
        YellPolicyFactory fact = new YellPolicyFactory();
        yell = fact.build();
    }
    // The goal of this exception is to abort a command by bubbling up to main
    public static class EndOfCommand extends Error {

        private static final long serialVersionUID = 1L;
    }

    // Description of what the command does
    public abstract String cmdname();
    public abstract String tagline();
    protected abstract String proto();
    public abstract void longDescription(IndentedWriter printer);
    // Abstract methods
    protected abstract List<Option> setOptions();
    public void perform(String[] args) {
        resetExitCode();
        // Default implementation could be override
        List<GeneralizedFile> arguments = getArgs(args);
        if (arguments != null) {
            perform(arguments);
        }
    }
    protected void longOpt(Options opt, String desc, String longName,
                           String shortName, String paramName) {
        OptionBuilder.withDescription(desc);
        OptionBuilder.hasArg();
        OptionBuilder.withArgName(paramName);
        OptionBuilder.withLongOpt(longName);
        opt.addOption(OptionBuilder.create(shortName));
    }
    public void perform(List<GeneralizedFile> args) {
        throw new NotImplementedException();
    }
    /** Prints the usage in case of bad usage by the user */
    public void usage() { // FIXME
        if (exitCode.getExitCode() != 0) {
            System.setOut(System.err);
        }
        initOptions();
        System.out.println(scat("dctc"
                                , cmdname()
                                , proto()));
        Usage.print(opts);
    }
    public FileBuilder getFileBuilder() {
        return builder;
    }
    public Command setFileBuilder(FileBuilder builder) {
        this.builder = builder;
        return this;
    }
    public void perform(GeneralizedFile[] args) {
        resetExitCode();
        perform(Arrays.asList(args));
    }
    public ExitCode getExitCode() {
        return exitCode;
    }
    public void setExitCode(ExitCode exitCode) {
        this.exitCode = exitCode;
    }
    public Command withExitCode(ExitCode exitCode) {
        setExitCode(exitCode);
        return this;
    }

    // Protected methods
    protected void parseCommandLine(String[] shellargs) {
        initOptions();
        parser = new Parser()
            .withOptions(opts);
        parser.parser(shellargs);
        if (parser.hasOption("help")) {
            Main.commandHelp(this, new IndentedWriter());
            throw new EndOfCommand();
        }
        if (parser.hasOption('v')) {
            Logger.getRootLogger().setLevel(Level.INFO);
        }
    }
    protected List<GeneralizedFile> getArgs(String[] shellargs) {
        parseCommandLine(shellargs);
        return resolveGlobbing(parser.getArgs());
    }
    protected List<GeneralizedFile> resolveGlobbing(List<String> args) {
        List<GeneralizedFile> gargs = new ArrayList<GeneralizedFile>();

        for (String arg: args) {
            GeneralizedFile garg = build(arg);
            if (GlobalConf.getResolveGlobbing()) {
                try {
                    gargs.addAll(Globbing.resolve(garg, false));
                }
                catch (IOException e) {
                    error(garg.givenName(),
                          "Couldn't resolve globbing for " + garg.givenName(), e, 2);
                    gargs.add(garg);
                }
            }
            else {
                gargs.add(garg);
            }
        }
        return gargs;
    }

    protected void setExitCode(int exitCode) {
        this.exitCode.setExitCode(exitCode);

    }
    protected void resetExitCode() {
        this.exitCode.resetExitCode();
    }

    protected void error(String msg, int exitCode) {
        yell.yell(cmdname(), msg, null);
        setExitCode(exitCode);
    }
    protected void error(String msg, Throwable exception, int exitCode) {
        yell.yell(cmdname(), msg, exception);
        setExitCode(exitCode);
    }
    protected void error(String fileName, String msg,
                         Throwable exception, int exitCode) {
        error(pquoted(fileName) + ": " + msg, exception, exitCode);
    }
    protected void error(GeneralizedFile file, String msg,
                         Throwable exception, int exitCode) {
        error(file.givenName(), msg, exception, exitCode);
    }
    protected void error(GeneralizedFile file, String msg, int exitCode) {
        error(file, msg, null, exitCode);
    }

    protected void errorWithHandlingOfKnownExceptions(String fileName,
                                                      String msg,
                                                      Throwable exception,
                                                      int exitCode) {
        msg = (fileName == null ? msg :  ("`" + fileName + "': " + msg));
        if (exception instanceof UnknownHostException) {
            error(msg + ": Unknown host '" + exception.getMessage() + "'", exitCode);
        }
        else {
            error(msg, exception, exitCode);
        }
    }

    protected void warn(String msg) {
        error(msg, 0);
    }
    protected void warn(String msg, Throwable exception) {
        error(msg, exception, 0);
    }

    protected GeneralizedFile build(String path) {
        return getFileBuilder().buildFile(path);
    }
    protected List<GeneralizedFile> build(String[] paths) {
        GeneralizedFile[] array = getFileBuilder().buildFile(paths);
        return Arrays.asList(array);
    }
    protected boolean hasOption(char opt) {
        return parser != null && parser.hasOption(opt);
    }
    protected boolean hasOption(String opt) {
        return parser != null && parser.hasOption(opt);
    }
    protected String getOptionValue(String opt) {
        return parser.getOptionValue(opt);
    }
    protected String getOptionValue(String opt, String defaultValue) {
        return hasOption(opt) ? getOptionValue(opt) : defaultValue;
    }
    protected String getOptionValue(char opt) {
        return parser.getOptionValue(opt);
    }
    protected String getOptionValue(char opt, String defaultValue) {
        return hasOption(opt) ? getOptionValue(opt) : defaultValue;
    }
    protected Option stdOption(char shortOpt,
                               String longOpt,
                               String descrip) {
        return stdOption(shortOpt, longOpt, descrip, false);
    }
    protected Option stdOption(String shortOpts,
                               String longOpt,
                               String descrip) {
        return stdOption(shortOpts, longOpt, descrip, false);
    }
    protected Option stdOption(char shortOpt,
                               String descrip) {
        ShortOption sopt = new ShortOption();
        sopt.addOpt(shortOpt);
        return new Option()
            .withShortOption(sopt)
            .withDescription(descrip);
    }
    protected Option stdOption(char shortOpt,
                               String longOpt,
                               String descrip,
                               boolean hasArg) {
        ShortOption sopt = new ShortOption();
        sopt.addOpt(shortOpt);
        LongOption lopt = new LongOption();
        lopt.addOpt(longOpt);
        return new Option()
            .withShortOption(sopt)
            .withLongOption(lopt)
            .withDescription(descrip)
            .withHasOption(hasArg);
    }
    protected Option stdOption(String shortOpts,
                               String longOpt,
                               String descrip,
                               boolean hasArg) {
        ShortOption sopt = new ShortOption();
        sopt.addOpts(shortOpts);
        LongOption lopt = new LongOption();
        lopt.addOpt(longOpt);
        return new Option()
            .withShortOption(sopt)
            .withLongOption(lopt)
            .withDescription(descrip)
            .withHasOption(hasArg);
    }
    protected List<String> getArgs() {
        return parser.getArgs();
    }
    // Private methods
    private void initOptions() {
        if (opts == null) {
            opts = setOptions();
            Option help = new Option()
            .withDescription("Display this help message.");
            LongOption longHelp = new LongOption();
            longHelp.addOpt("help");
            help.setLongOption(longHelp);

            opts.add(help);
        }
    }
    // Attributes
    private Parser parser;
    private List<Option> opts;
    private FileBuilder builder;
    protected YellPolicy yell;
    private ExitCode exitCode;
}
