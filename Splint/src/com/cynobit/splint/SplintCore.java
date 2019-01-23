package com.cynobit.splint;

import java.io.File;
import java.util.List;

/**
 * (c) CynoBit 2019
 * Created by Francis Ilechukwu 1/22/2019.
 */
public class SplintCore {

    /**
     * @param identifier
     */
    static void installPackage(String identifier) {
        System.out.println(String.format("Requesting package %s ...", identifier));
    }

    /**
     * @param packages
     */
    static void installPackages(List<String> packages) {
        for (String identifier : packages) {
            System.out.println(String.format("Requesting package %s ...", identifier));

        }
    }
    private static boolean packageExistsLocaly(String identifier) {
        File file = new File(Main.appRoot);
        return false;
    }
}
