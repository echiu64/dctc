package com.dataiku.dctc.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.dataiku.dctc.DCTCLog;
import com.dataiku.dctc.Globbing;
import com.dataiku.dctc.Main;
import com.dataiku.dctc.configuration.Configuration;
import com.dataiku.dctc.configuration.GlobalConf;
import com.dataiku.dctc.file.FileBuilder;
import com.dataiku.dctc.file.GeneralizedFile;

public abstract class Command {
    // The goal of this exception is to abort a command by bubbling up to main
    public static class EndOfCommandException extends Error {
        private static final long serialVersionUID = 1L;
    }
    
    // Description of what the command does
    public abstract String cmdname();
    public abstract String tagline();
    protected abstract String proto();
    public abstract String longDescription();

    // Abstract methods
    protected abstract Options setOptions();
    protected List<String> getFileArguments(String[] list) {
        List<String> res = new ArrayList<String>();
        for (String l: list) {
            res.add(l);
        }
        return res;
    }
    public void perform(String[] args) throws IOException {
        // Default implementation could be override
        List<GeneralizedFile> arguments = getArgs(args);
        if (arguments != null) {
            perform(arguments);
        }
    }
    @SuppressWarnings("static-access")
    protected void longOpt(Options opt, String desc, String optArgName, String optName) {
        OptionBuilder.withDescription(desc);
        OptionBuilder.hasArg();
        opt.addOption(OptionBuilder.withLongOpt(optArgName).create(optName));
    }
    public void perform(List<GeneralizedFile> args) throws IOException {
        throw new NotImplementedException();
    }

    /** Prints the usage in case of bad usage by the user */
    public void usage() {
        if (getExitCode() != 0) {
            System.setOut(System.err);
        }
        initOptions();
        HelpFormatter formatter = new HelpFormatter();
        formatter.setDescPadding(10);
        formatter.printHelp(proto(), opt);

    }
    public FileBuilder getFileBuilder() {
        return builder;
    }
    public Command setFileBuilder(FileBuilder builder) {
        this.builder = builder;
        return this;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public int getExitCode() {
        return exitCode;
    }
    public void perform(GeneralizedFile[] args) throws IOException {
        List<GeneralizedFile> gargs = new ArrayList<GeneralizedFile>();
        for (GeneralizedFile arg: args) {
            gargs.add(arg);
        }
        perform(gargs);
    }

    // Protected methods

    protected void parseCommandLine(String[] shellargs) {
        if (line != null) {
            System.err.println("Double call to parseCommandLine");
        }
        initOptions();
        CommandLineParser parser = new PosixParser();
        try {
            line = parser.parse(opt, shellargs);
        } catch (ParseException exp) {
            error(exp.getMessage(), 1);
            throw new EndOfCommandException();

        }
        if (line.hasOption("help")) {
            Main.commandHelp(this);
            throw new EndOfCommandException();
        }
        if (line.hasOption("v")) {
            Logger.getRootLogger().setLevel(Level.INFO);
        }
        if (line.hasOption("V")) {
            Logger.getRootLogger().setLevel(Level.DEBUG);
        }
    }

    protected List<GeneralizedFile> getArgs(String[] shellargs) throws IOException{
        parseCommandLine(shellargs);
        List<GeneralizedFile> args = new ArrayList<GeneralizedFile>();
        for (String arg: getFileArguments(line.getArgs())) {
            GeneralizedFile garg = build(arg);
            if (GlobalConf.getResolveGlobbing()) {
                args.addAll(Globbing.resolve(garg, false));
            } else {
                args.add(garg);
            }
        }
        return args;
    }
    protected void error(String msg, int exitCode) {
        DCTCLog.error(cmdname(), msg);
        exitCode(exitCode);
    }
    protected void error(String msg, Throwable exception, int exitCode) {
        DCTCLog.error(cmdname(), msg, exception);
        exitCode(exitCode);
    }
    protected void exitCode(int exitCode) {
        this.exitCode = Math.max(exitCode, this.exitCode);
    }
    protected void error(String fileName, String msg, int errorCode) {
        error("`" + fileName + "' failed: " + msg, errorCode);
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
        List<GeneralizedFile> res = new ArrayList<GeneralizedFile>();
        for (GeneralizedFile a: array) {
            res.add(a);
        }
        return res;
    }
    protected boolean hasOption(String opt) {
        return line != null && line.hasOption(opt);
    }
    protected String getOptionValue(String opt) {
        return line.getOptionValue(opt);
    }
    protected String getOptionValue(String opt, String defaultValue) {
        return hasOption(opt) ? getOptionValue(opt) : defaultValue;
    }

    // Private methods
    private void initOptions() {
        if (opt == null) {
            opt = setOptions();
            OptionBuilder.withDescription("Display this help message.");
            opt.addOption(OptionBuilder.withLongOpt("help").create());
            // opt.addOption("help", false, );
            opt.addOption("v", "verbose", false, "Enable verbose logging");
            opt.addOption("V", "VV", false, "Enable debug logging");
        }
    }

    // Attributes
    private CommandLine line;
    private int exitCode;
    private FileBuilder builder;
    protected Configuration configuration;
    private Options opt;
}