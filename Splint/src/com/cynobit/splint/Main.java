package com.cynobit.splint;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * (c) CynoBit 2019
 * Created by Francis Ilechukwu on 1/22/2019.
 */
public class Main {

    private final static String ENVIRONMENT = "DEBUG";

    static String appRoot;

    @Option(name = "-i", aliases = "--install", usage = "Installs a splint package.")
    private String packageIdentifier;

    @Option(name = "-p", aliases = {"--patch", "--install-sdk"}, usage = "Installs or Patch your current Code-Igniter distribution")
    private boolean patch;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Argument
    private List<String> specialArgs = new ArrayList<>();

    public static final String APP_NAME = "Splint Client";

    public static void main(String[] args) {
        final Main instance = new Main();
        try {
            appRoot = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + "\\";
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            System.exit(0);
        }
        try {
            parser.parseArgument(arguments);
            // >_splint install vendor/package .../... .../...
            if (specialArgs.size() > 0) {
                if (specialArgs.get(0).equals("install")) {
                    if (specialArgs.size() == 2 && specialArgs.get(1).matches("(\\w+)/([a-zA-Z0-9_\\-]+)")) {
                        SplintCore.installPackage(specialArgs.get(1));
                    } else {
                        List<String> packages = new ArrayList<>();
                        for (int x = 1; x < specialArgs.size(); x++) {
                            if (specialArgs.get(x).matches("(\\w+)/([a-zA-Z0-9_\\-]+)")) {
                                packages.add(specialArgs.get(x));
                            } else {
                                System.out.println("Invalid package name: " + specialArgs.get(x));
                                System.exit(3);
                            }
                        }
                        SplintCore.installPackages(packages);
                    }
                }
            }
            // >_splint -i vendor/package .../... .../...
            if (packageIdentifier != null && packageIdentifier.matches("(\\w+)/([a-zA-Z0-9_\\-]+)")) {
                SplintCore.installPackage(packageIdentifier);
                for (int x = 0; x < specialArgs.size(); x++) {
                    if (specialArgs.get(x).matches("(\\w+)/([a-zA-Z0-9_\\-]+)")) {
                        SplintCore.installPackage(specialArgs.get(x));
                    } else {
                        System.out.println("Invalid package name: " + specialArgs.get(x));
                        System.exit(3);
                    }
                }
            }
        } catch (CmdLineException e) {
            System.out.println("Unable to parse command line arguments");
            System.exit(ExitCodes.BAD_ARGUMENTS);
        }
        System.exit(0);
    }

    @SuppressWarnings("ConstantConditions")
    public static void printLog(String message) {
        if (ENVIRONMENT.equals("DEBUG")) {
            System.out.println(message);
        }
    }
}
