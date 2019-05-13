package com.cynobit.splint;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * (c) CynoBit 2019
 * Created by Francis Ilechukwu on 1/22/2019.
 */
public class Main {

    private final static String BUILD_VERSION = "0.0.6";
    private final static String ENVIRONMENT = "PRODUCTION";

    static String appRoot;

    @Option(name = "-v", aliases = {"--version"}, usage = "Shows the current version of your Splint package manager.")
    private boolean showVersion = false;

    @Option(name = "-i", aliases = "--install", usage = "Installs a splint package.")
    private String packageIdentifier;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(name = "-n", aliases = {"--no-patch", "--no-install-sdk"}, usage = "Do not patch the loader for the current distribution")
    private boolean noPatch = false;

    @Option(name = "-f", aliases = {"--force-patch"}, usage = "Forcefully patches your distribution even if the Loader class already exists.")
    private boolean forcePatch = false;

    @Option(name = "-l", aliases = {"--list-packages"}, usage = "Lists all the packages installed in the current Code Igniter distribution.")
    private boolean listAllPackages = false;

    @Option(name = "-c", aliases = {"--create-package", "--create-library"}, usage = "Creates a new splint package in a  new folder specified by 'vendor/package_name' under the folder 'application/splints'.")
    private String newPackage;

    @Option(name = "-r", aliases = {"--read-me"}, usage = "Create README File when creating package.")
    private boolean readMe;

    @Option(name = "-p", aliases = {"--patch"}, usage = "Patches a code-igniter distributable..")
    private boolean patch = false;

    @Option(name = "-b", aliases = {"--begin-new-project"}, usage = "Creates a Code-Igniter distribution in a folder with the given argument as the folder name.")
    private String projectName;


    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Argument
    private List<String> specialArgs = new ArrayList<>();

    public static final String APP_NAME = "Splint Client";

    public static void main(String[] args) {
        final Main instance = new Main();
        try {
            appRoot = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + "\\";
            appRoot = appRoot.replace("\\splint.exe", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            instance.processArguments(args);
        } catch (IOException e) {
            System.out.println("I/O Exception encountered");
            System.exit(ExitCodes.IO_EXCEPTION);
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
                    File dir = new File(System.getProperty("user.dir") + "/application");
                    if (!dir.isDirectory()) {
                        System.out.println("Could not locate application folder, Please run this command in a code igniter root or one with the application folder");
                        System.exit(ExitCodes.NO_APPLICATION_FOLDER);
                    }
                    List<String> packages = new ArrayList<>();
                    if (specialArgs.size() > 1) {
                        for (int x = 1; x < specialArgs.size(); x++) {
                            if (specialArgs.get(x).matches("(\\w+)/([a-zA-Z0-9_\\-]+)")) {
                                packages.add(specialArgs.get(x));
                            } else {
                                System.out.println("Invalid package name: " + specialArgs.get(x));
                                System.exit(ExitCodes.INVALID_PACKAGE_NAME);
                            }
                        }
                    } else {
                        File splintFile = new File(System.getProperty("user.dir") + "/splint.json");
                        if (!splintFile.isFile()) {
                            System.out.println("No 'splint.json' file found.");
                            System.exit(0);
                        }
                        System.out.println("Reading 'splint.json' file");
                        FileReader fileReader = new FileReader(splintFile);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                        String line;
                        StringBuilder builder = new StringBuilder();
                        while ((line = bufferedReader.readLine()) != null) {
                            builder.append(line);
                        }
                        bufferedReader.close();
                        try {
                            JSONObject splintJSON = new JSONObject(builder.toString());
                            if (splintJSON.has("install")) {
                                JSONArray installArray = splintJSON.getJSONArray("install");
                                for (int x = 0; x < installArray.length(); x++) {
                                    if (installArray.getString(x).matches("(\\w+)/([a-zA-Z0-9_\\-]+)")) {
                                        System.out.println("Found package " + installArray.getString(x));
                                        packages.add(installArray.getString(x));
                                    } else {
                                        System.err.println("Invalid package name in splint.json file: " + installArray.getString(x));
                                        System.exit(ExitCodes.INVALID_PACKAGE_NAME);
                                    }
                                }
                            } else {
                                System.out.println("No packages to install.");
                            }
                        } catch (Exception e) {
                            System.exit(ExitCodes.ERROR_PROCESSING_SPLINT_FILE);
                        }
                    }
                    List<String> installedPackages = new ArrayList<>();
                    List<String> dependencies = SplintCore.getDependencies(SplintCore.installPackages(packages));
                    installedPackages.addAll(packages);
                    while (dependencies.size() > 0) {
                        for (String _package : installedPackages) {
                            if (dependencies.contains(_package)) {
                                dependencies.remove(_package);
                            }
                        }
                        dependencies = SplintCore.getDependencies(SplintCore.installPackages(dependencies));
                        installedPackages.addAll(dependencies);
                    }
                    System.out.println("Updating Splint File...");
                    SplintCore.refreshRootSplintJSONFile();
                    System.out.println("Done Installing Packages.");
                } else {
                    parser.printUsage(System.out);
                    System.exit(0);
                }
            }
            // >_splint -i vendor/package .../... .../...
            if (packageIdentifier != null) {
                if (packageIdentifier.matches("(\\w+)/([a-zA-Z0-9_\\-]+)")) {
                    List<String> packages = new ArrayList<>();
                    packages.add(packageIdentifier);
                    for (String specialArg : specialArgs) {
                        if (specialArg.matches("(\\w+)/([a-zA-Z0-9_\\-]+)")) {
                            packages.add(specialArg);
                        } else {
                            System.out.println("Invalid package name: " + specialArg);
                            System.exit(ExitCodes.INVALID_PACKAGE_NAME);
                        }
                    }
                    List<String> installedPackages = new ArrayList<>();
                    List<String> dependencies = SplintCore.getDependencies(SplintCore.installPackages(packages));
                    installedPackages.addAll(packages);
                    while (dependencies.size() > 0) {
                        for (String _package : installedPackages) {
                            if (dependencies.contains(_package)) {
                                dependencies.remove(_package);
                            }
                        }
                        dependencies = SplintCore.getDependencies(SplintCore.installPackages(dependencies));
                        installedPackages.addAll(dependencies);
                    }
                    System.out.println("Updating Splint File...");
                    SplintCore.refreshRootSplintJSONFile();
                    System.out.println("Done Installing Packages.");
                } else {
                    System.out.println("Invalid package name: " + packageIdentifier);
                    System.exit(ExitCodes.INVALID_PACKAGE_NAME);
                }
            } else if (listAllPackages) {
                noPatch = true;
                SplintCore.listPackages(false);
            } else if (newPackage != null) {
                noPatch = true;
                SplintCore.createPackage(newPackage, readMe);
            } else if (patch) {
                noPatch = true;
                SplintCore.patchLoader(true);
            } else if (projectName != null) {
                SplintCore.createCIProject(projectName, noPatch);
                noPatch = true;
            } else if (showVersion) {
                noPatch = true;
                System.out.println("Splint v" + Main.BUILD_VERSION);
            }
            if (!noPatch) SplintCore.patchLoader(forcePatch);
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
