package com.cynobit.splint;

import com.cynobit.splint.models.CloudManager;
import com.cynobit.splint.models.DataSource;
import javafx.util.Pair;
import net.lingala.zip4j.core.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * (c) CynoBit 2019
 * Created by Francis Ilechukwu 1/22/2019.
 */
class SplintCore {

    private static final CloudManager cloudManager = CloudManager.getInstance();
    private static final DataSource dataSource = new DataSource(Main.appRoot);

    static void installPackages(List<String> packageList) {
        List<String> toDownload = new ArrayList<>();
        List<String> toInstall = new ArrayList<>();
        List<String> noInstall = new ArrayList<>();
        for (String identifier : packageList) {
            System.out.println(String.format("Reading cache for package %s ...", identifier));
            if (!packageExistsLocally(identifier)) {
                toDownload.add(identifier);
            }
        }
        if (toDownload.size() > 0) {
            System.out.println("Requesting packages...");
            cloudManager.requestPackages(toDownload, new CloudManager.CloudResponseListener() {
                @Override
                public void onResponseReceived(String response) {
                    try {
                        JSONObject object = new JSONObject(response);
                        if (object.getInt("code") == 1) {
                            JSONArray packages = object.getJSONArray("data");
                            for (int x = 0; x < packages.length(); x++) {
                                if (!downloadPackage(packages.getJSONObject(x).getString("identifier"),
                                        packages.getJSONObject(x).getString("integrity"))) System.exit(6);
                                if (!dataSource.isConnected()) {
                                    if (!dataSource.connect()) System.exit(ExitCodes.DB_CONNECTION_ERROR);
                                }
                                if (!dataSource.updatePackageInfo(packages.getJSONObject(x).getString("identifier"),
                                        packages.getJSONObject(x).getString("version"),
                                        packages.getJSONObject(x).getInt("id"),
                                        packages.getJSONObject(x).getString("integrity")))
                                    System.out.println("Error updating cache, continuing...");
                                toInstall.add(packages.getJSONObject(x).getString("identifier"));
                            }
                            for (String _package : toDownload) {
                                if (!inList(_package, toInstall)) noInstall.add(_package);
                            }
                            for (String _package : packageList) {
                                if (!inList(_package, noInstall) && !inList(_package, toInstall))
                                    toInstall.add(_package);
                            }
                            if (noInstall.size() > 0) {
                                System.out.print("The following package(s) could not be found:");
                                for (String _package : noInstall) {
                                    System.out.println(_package);
                                }
                                System.out.println("Continuing...");
                            }
                            synchronized (cloudManager) {
                                cloudManager.notifyAll();
                            }
                        } else {
                            System.out.println("Splint is currently experiencing technical issues.");
                            System.exit(ExitCodes.TECHNICAL_ISSUE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServerError(int responseCode) {

                }

                @Override
                public void onNetworkError() {

                }
            });
            synchronized (cloudManager) {
                try {
                    cloudManager.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (String _package : toInstall) {
            if (!installPackage(_package)) {
                System.err.println("Error Installing package: " + _package);
                System.exit(ExitCodes.ERROR_INSTALL_PACKAGE);
            }
        }
        System.out.println("Done Installing Packages.");
    }

    private static boolean installPackage(String identifier) {
        System.out.println(String.format("Installing package: %s ...", identifier));
        File file = new File(System.getProperty("user.dir") + "/application/splints/" + identifier);
        if (!file.isDirectory()) {
            if (!file.mkdirs()) {
                System.out.println("Could not create the package folder");
                System.exit(ExitCodes.SPLINT_FOLDER_CREATE_FAILED);
            }
        }
        try {
            ZipFile zipFile = new ZipFile(Main.appRoot + "packages/" + identifier + ".zip");
            zipFile.extractAll(System.getProperty("user.dir") + "/application/splints/" + identifier);
        } catch (Exception e) {
            System.err.println("Problem extracting package: " + identifier);
            System.exit(ExitCodes.EXTRACTION_ERROR);
        }
        System.out.println("Done Installing package: " + identifier);
        return true;
    }

    private static boolean inList(String string, List<String> list) {
        for (String item : list) {
            if (item.equals(string)) return true;
        }
        return false;
    }

    private static String getMD5Checksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        byte[] b = complete.digest();
        StringBuilder result = new StringBuilder();
        for (byte aB : b) {
            result.append(Integer.toString((aB & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    private static boolean downloadPackage(String identifier, String integrity) {
        try {
            URL url = new URL(CloudManager.API + "downloadPackage");
            HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("User-Agent", CloudManager.USER_AGENT);
            httpConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            String urlParameters = "identifier=" + identifier;

            // Send post request
            httpConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(httpConnection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            long completeFileSize = httpConnection.getContentLength();
            BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
            File folder = new File(Main.appRoot + "packages/" + identifier.substring(0, identifier.indexOf("/")));
            if (!folder.exists()) {
                if (!folder.mkdir()) System.exit(7);
            }
            FileOutputStream fos = new FileOutputStream(Main.appRoot + "packages/" + identifier + ".zip");
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
            byte[] data = new byte[1024];
            long downloadedFileSize = 0;
            int x;
            while ((x = in.read(data, 0, 1024)) >= 0) {
                downloadedFileSize += x;
                final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 100d);
                System.out.write(("\rDownloading package: " + identifier + " " + currentProgress + "%").getBytes());
                bout.write(data, 0, x);
            }
            bout.close();
            in.close();
            return integrity.equals(getMD5Checksum(Main.appRoot + "packages/" + identifier + ".zip"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean packageExistsLocally(String identifier) {
        if (!dataSource.isConnected()) {
            if (!dataSource.connect()) return false;
        }
        int localVersion = dataSource.getPackageVersionId(identifier);
        if (localVersion == -1) return false;
        final int[] remoteVersionId = {-1};
        cloudManager.getLatestVersion(identifier, new CloudManager.CloudResponseListener() {
            @Override
            public void onResponseReceived(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    if (object.getInt("code") == 1) {
                        remoteVersionId[0] = object.getInt("data");
                    }
                    synchronized (cloudManager) {
                        cloudManager.notifyAll();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServerError(int responseCode) {
                System.exit(4);
            }

            @Override
            public void onNetworkError() {
                System.exit(5);
            }
        });
        synchronized (cloudManager) {
            try {
                cloudManager.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return !(remoteVersionId[0] > dataSource.getPackageVersionId(identifier));
    }
}
