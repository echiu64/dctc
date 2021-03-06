package com.dataiku.dctc.command;

import java.util.List;

import com.dataiku.dctc.clo.LongOption;
import com.dataiku.dctc.clo.OptionAgregator;
import com.dataiku.dctc.command.abs.Command;
import com.dataiku.dctc.command.cat.AlgorithmType;
import com.dataiku.dctc.command.cat.AlwaysCatHeaderSelector;
import com.dataiku.dctc.command.cat.CatAlgorithmFactory;
import com.dataiku.dctc.command.cat.CatRunner;
import com.dataiku.dctc.command.cat.NeverCatHeaderSelector;
import com.dataiku.dctc.file.GFile;
import com.dataiku.dip.utils.IndentedWriter;

public class Tail extends Command {
    public String tagline() {
        return "Output the end of files.";
    }
    public void longDescription(IndentedWriter printer) {
        printer.print("Output the last N lines or bytes of the input files");
    }
    @Override
    public String cmdname() {
        return "tail";
    }
    @Override
    protected String proto() {
        return "[OPTIONS...] [FILE...]";
    }
    @Override
    protected void setOptions(List<OptionAgregator> opts) {
        opts.add(stdOption('c'
                           , "bytes"
                           , "Output the last K bytes."
                           , true
                           , "K"));
        opts.add(stdOption('n'
                           , "lines"
                           , "Output the last K lines."
                           , true
                           , "K"));
        opts.add(stdOption('q'
                           , "quiet"
                           , "Never output headers giving file names.")
                 .withOpt(new LongOption().withOpt("silent")));
    }
    @Override
    public void perform(List<GFile> args) {
        CatAlgorithmFactory fact = new CatAlgorithmFactory()
            .withAlgo(AlgorithmType.TAIL)
            .withSkipFirst(number())
            .withIsLineAlgo(isLine())
            .withYell(getYell());

        CatRunner runner = new CatRunner()
            .withYell(getYell());

        if (args.size() > 1 && !hasOption("-quiet")) {
            runner.setHeader(new AlwaysCatHeaderSelector());
        }
        else {
            runner.setHeader(new NeverCatHeaderSelector());
        }

        runner.perform(args, fact, getExitCode(), true);
    }
    public long number() {
        nbLines();
        nbBytes();

        return number;
    }
    public void nbLines() {
        if (isLine()) {
            number = getIntOption('n', 10);
        }
    }
    public void nbBytes() {
        if (!isLine()) {
            number = getIntOption('c', 10);
        }
    }
    private boolean isLine() {
        if (isLine == null) {
            String line = getLastPosition("-lines", "-bytes");
            if (line != null) {
                isLine = line.equals("-lines");
            }
            else {
                isLine = true;
            }
        }

        return isLine;
    }

    private Boolean isLine;
    private long number = -1;
}
