package com.cynobit.splint;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;

/**
 * (c) CynoBit 2019
 * Created by Francis Ilechukwu on 1/22/2019.
 */
public class Main {

    @Option(name = "-i", aliases = "--install", usage = "Installs a splint package.")
    private String packageIdentifier;

    @Option(name = "-p", aliases = {"--patch", "--install-sdk"}, usage = "Installs or Patch your current Code-Igniter distribution")
    private boolean patch;

    public static void main(String[] args) {
        final Main instance = new Main();
        try {
            instance.processArguments(args);
        } catch (IOException e) {
            System.out.println("I/O Exception encountered");
        }
        System.exit(0);
    }

    private void processArguments(String[] arguments) throws IOException {
        final CmdLineParser parser = new CmdLineParser(this);
        if (arguments.length < 1) {
            parser.printUsage(System.out);
            System.exit(1);
        }
        try {
            parser.parseArgument(arguments);
            System.exit(2);
        } catch (CmdLineException e) {
            System.out.println("Unable to parse command line arguments");
        }
    }
}
