package com.cynobit.splint_update;

import com.cynobit.splint_update.models.CloudManager;
import com.cynobit.splint_update.models.Preferences;
import javafx.util.Pair;
import net.lingala.zip4j.core.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
//import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;

/**
 * Created by Francis Ilechukwu 2/25/2019.
 */
public class Main {

    private final static String BUILD_VERSION = "0.0.3";
    private final static String ENVIRONMENT = "PRODUCTION";

    private static final CloudManager cloudManager = CloudManager.getInstance();
    private static String appRoot = null;
    private static Preferences preferences;

    public static void main(String[] args) {
        // Initialize
        try {
            appRoot = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + "\\";
            appRoot = appRoot.replace("\\splint-update.exe", "");
        } catch (Exception e) {
            System.err.println("Could not get app root.");
            System.exit(ExitCodes.ROOT_PATH_ERROR);
        }
        preferences = new Preferences(appRoot);
        // Get Current Version of Splint Binary.
        System.out.println("Checking for updates...");
        String residentVersion = "0.0.0";
        final String[] cloudVersion = {"0.0.0"};
        final String[] updateHash = {""};
        try {
            Process process = Runtime.getRuntime().exec("cmd.exe /c splint -v");
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Splint")) {
                    residentVersion = line.substring(line.indexOf("v") + 1).trim();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Get Version of Splint Binary in the Cloud.
        cloudManager.getLatestDistributableVersion(new CloudManager.CloudResponseListener() {
            @Override
            public void onResponseReceived(String response, ArrayList<Pair<String, String>> headers) {
                try {
                    JSONObject object = new JSONObject(response);
                    cloudVersion[0] = object.getString("response");
                    synchronized (cloudManager) {
                        cloudManager.notifyAll();
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing response from server.");
                    System.exit(ExitCodes.BAD_VERSION_RESPONSE);
                }
            }

            @Override
            public void onServerError(int responseCode) {
                System.err.println("There was an error communicating with the server");
                System.exit(ExitCodes.SERVER_ERROR);
            }

            @Override
            public void onNetworkError() {
                System.err.println("Could not resolve host name.");
                System.exit(ExitCodes.HOST_NAME_ERROR);
            }
        });
        synchronized (cloudManager) {
            try {
                cloudManager.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Compare Local and Cloud Binary Versions.
        if (cloudVersion[0].compareToIgnoreCase(residentVersion) > 0) {
            System.out.println("Update Found!");
            // Do Update: Get Update Patch Integrity SHA-1.
            cloudManager.getLatestDistributionHash(new CloudManager.CloudResponseListener() {
                @Override
                public void onResponseReceived(String response, ArrayList<Pair<String, String>> headers) {
                    try {
                        JSONObject object = new JSONObject(response);
                        updateHash[0] = object.getString("response");
                        synchronized (cloudManager) {
                            cloudManager.notifyAll();
                        }
                    } catch (Exception e) {
                        System.err.println("Error in reading update patch hash");
                        System.exit(ExitCodes.BAD_HASH_RESPONSE);
                    }
                }

                @Override
                public void onServerError(int responseCode) {
                    System.err.println("There was an error communicating with the server");
                    System.exit(ExitCodes.SERVER_ERROR);
                }

                @Override
                public void onNetworkError() {
                    System.err.println("Could not resolve host name.");
                    System.exit(ExitCodes.HOST_NAME_ERROR);
                }
            });
            synchronized (cloudManager) {
                try {
                    cloudManager.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Download Update Patch.
            try {
                URL url = new URL(CloudManager.BIN_API + "getUpdatePatch");
                HttpsURLConnection httpsConnection = (HttpsURLConnection) (url.openConnection());
                httpsConnection.setRequestMethod("GET");
                httpsConnection.setRequestProperty("User-Agent", CloudManager.USER_AGENT);
                httpsConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                long completeFileSize = httpsConnection.getContentLength();
                BufferedInputStream in = new BufferedInputStream(httpsConnection.getInputStream());
                File folder = new File(appRoot + "updates");
                if (!folder.exists()) {
                    if (!folder.mkdir()) System.exit(7);
                }
                FileOutputStream fos = new FileOutputStream(appRoot + "updates/" + "patch-" + cloudVersion[0] + ".zip");
                BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                byte[] data = new byte[1024];
                long downloadedFileSize = 0;
                int x;
                while ((x = in.read(data, 0, 1024)) >= 0) {
                    downloadedFileSize += x;
                    final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 100d);
                    System.out.write(("\rDownloading Update v" + cloudVersion[0] + " " + currentProgress + "%").getBytes());
                    bout.write(data, 0, x);
                }
                System.out.println();
                bout.close();
                in.close();
                System.out.println("Installing Update...");
                if (getMD5Checksum(appRoot + "updates/" + "patch-" + cloudVersion[0] + ".zip").equals(updateHash[0])) {
                    installPatch(cloudVersion[0]);
                } else {
                    System.err.println("Hash un-matched, possible MITM attack!");
                    System.exit(ExitCodes.MITM_ATTACK_ERROR);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Splint was successfully updated.");
            System.exit(0);
        } else {
            System.out.println("No Updates Found.");
        }
        //<editor-fold desc="Splint SDK Update">
        // SDK Patch Update
        System.out.println("Updating Splint SDK...");
        cloudManager.getLatestSDKVersion(new CloudManager.CloudResponseListener() {
            @Override
            public void onResponseReceived(String response, ArrayList<Pair<String, String>> headers) {
                try {
                    JSONObject object = new JSONObject(response);
                    if (object.has("version")) {
                        if (object.getString("version").compareToIgnoreCase(preferences.getSDKVersion()) > 0) {
                            downloadFile(
                                    "https://github.com/splintci/sdk/archive/" + object.getString("version") + ".zip",
                                    appRoot + "modifiers/", "splint-sdk.zip",
                                    "Splint SDK " + object.getString("version"));
                            preferences.setSDKVersion(object.getString("version"));
                            preferences.commit();
                            System.out.println("Splint SDK Updated.");
                        } else {
                            System.out.println("SDK already up to date.");
                        }
                    } else {
                        System.out.println("Could not obtain recent SDK version.");
                    }
                } catch (Exception e) {
                    System.err.println("There was an error parsing the server response.");
                }
                synchronized (cloudManager) {
                    cloudManager.notifyAll();
                }
            }

            @Override
            public void onServerError(int responseCode) {
                System.err.println("There was an error communicating with the server");
                System.exit(ExitCodes.SERVER_ERROR);
            }

            @Override
            public void onNetworkError() {
                System.err.println("Could not resolve host name.");
                System.exit(ExitCodes.HOST_NAME_ERROR);
            }
        });
        synchronized (cloudManager) {
            try {
                cloudManager.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //</editor-fold>
        //<editor-fold desc="Loader Patch Update">
        // Loader Patch Update
        System.out.println("Updating Splint Loader...");
        cloudManager.getLatestLoaderPatch(preferences.getLoaderSha(), new CloudManager.CloudResponseListener() {
            @Override
            public void onResponseReceived(String response, ArrayList<Pair<String, String>> headers) {
                if (!response.equals("*")) {
                    try {
                        FileOutputStream outputStream = new FileOutputStream(appRoot + "modifiers/MY_Loader.php", false);
                        byte[] strToBytes = DatatypeConverter.parseBase64Binary(response);
                        outputStream.write(strToBytes);
                        outputStream.close();
                        preferences.setLoaderSha(headers.get(0).getValue());
                        preferences.commit();
                        System.out.println("Splint Loader Updated.");
                    } catch (Exception e) {
                        System.err.println("Error Updating Patch File.");
                        System.exit(ExitCodes.PATCH_UPDATE_ERROR);
                    }
                } else {
                    System.out.println("Splint Loader already up to date.");
                }
                synchronized (cloudManager) {
                    cloudManager.notifyAll();
                }
            }

            @Override
            public void onServerError(int responseCode) {
                System.err.println("There was an error communicating with the server");
                System.exit(ExitCodes.SERVER_ERROR);
            }

            @Override
            public void onNetworkError() {
                System.err.println("Could not resolve host name.");
                System.exit(ExitCodes.HOST_NAME_ERROR);
            }
        });
        synchronized (cloudManager) {
            try {
                cloudManager.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //</editor-fold>
        //<editor-fold desc="URI Patch Update">
        // URI Patch Update.
        System.out.println("Updating URI Patcher...");
        cloudManager.getLatestURIPatch(preferences.getUriSha(), new CloudManager.CloudResponseListener() {
            @Override
            public void onResponseReceived(String response, ArrayList<Pair<String, String>> headers) {
                if (!response.equals("*")) {
                    try {
                        FileOutputStream outputStream = new FileOutputStream(appRoot + "modifiers/MY_Uri.php", false);
                        byte[] strToBytes = DatatypeConverter.parseBase64Binary(response);
                        outputStream.write(strToBytes);
                        outputStream.close();
                        preferences.setUriSha(headers.get(0).getValue());
                        preferences.commit();
                        System.out.println("URI Patcher Updated.");
                    } catch (Exception e) {
                        System.err.println("Error URI Patcher File.");
                        System.exit(ExitCodes.PATCH_UPDATE_ERROR);
                    }
                } else {
                    System.out.println("URI Patcher already up to date.");
                }
                synchronized (cloudManager) {
                    cloudManager.notifyAll();
                }
            }

            @Override
            public void onServerError(int responseCode) {
                System.err.println("There was an error communicating with the server");
                System.exit(ExitCodes.SERVER_ERROR);
            }

            @Override
            public void onNetworkError() {
                System.err.println("Could not resolve host name.");
                System.exit(ExitCodes.HOST_NAME_ERROR);
            }
        });
        synchronized (cloudManager) {
            try {
                cloudManager.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //</editor-fold>
        System.out.println("Your Splint installation is now up to date.");
    }

    private static void installPatch(String version) {
        try {
            ZipFile zipFile = new ZipFile(appRoot + "updates/patch-" + version + ".zip");
            zipFile.extractAll(appRoot + "updates/sink");
        } catch (Exception e) {
            System.err.println("Problem extracting patch");
            System.exit(ExitCodes.EXTRACTION_ERROR);
        }
        try {
            JSONObject manifest = readJSONFromFile(appRoot + "updates/sink/manifest.json");
            if (!manifest.getString("version").equals(version)) {
                System.err.println("Bad Patch File.");
                System.exit(ExitCodes.MANIFEST_ERROR);
            }
            JSONArray patches = manifest.getJSONArray("patches");
            for (int x = 0; x < patches.length(); x++) {
                JSONArray patch = patches.getJSONArray(x);
                if (getMD5Checksum(appRoot + "updates/sink/" + patch.getString(0)).equals(
                        patch.getString(2)
                )) {
                    Files.copy(new File(appRoot + "updates/sink/" + patch.getString(0)).toPath(),
                            new File(appRoot + patch.get(1)).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    System.err.println("Patch seems to have been tampered with.");
                    System.exit(ExitCodes.PATCH_TAMPER_ERROR);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading patch manifest");
            System.exit(ExitCodes.BAD_PATCH_FILE);
        }
    }

    private static void downloadFile(String remoteUrl, String destinationDir, String destinationName, String itemName) {
        try {
            URL url = new URL(remoteUrl);
            HttpsURLConnection httpsConnection = (HttpsURLConnection) (url.openConnection());
            httpsConnection.setRequestMethod("GET");
            httpsConnection.setRequestProperty("User-Agent", CloudManager.USER_AGENT);
            httpsConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            long completeFileSize = httpsConnection.getContentLength();
            BufferedInputStream in = new BufferedInputStream(httpsConnection.getInputStream());
            File folder = new File(destinationDir);
            if (!folder.exists()) {
                if (!folder.mkdir()) System.exit(7);
            }
            FileOutputStream fos = new FileOutputStream(destinationDir + destinationName);
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
            byte[] data = new byte[1024];
            long downloadedFileSize = 0;
            int x;
            while ((x = in.read(data, 0, 1024)) >= 0) {
                downloadedFileSize += x;
                final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 100d);
                System.out.write(("\rDownloading " + itemName + " " + currentProgress + "%").getBytes());
                bout.write(data, 0, x);
            }
            System.out.println();
            bout.close();
            in.close();
        } catch (Exception e) {
            System.err.println("Error Downloading " + itemName);
            System.exit(ExitCodes.ERROR_IN_DOWNLOAD);
        }
    }

    private static JSONObject readJSONFromFile(String path) {
        try {
            String line;
            StringBuilder builder = new StringBuilder();
            FileReader fileReader = new FileReader(new File(path));
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            bufferedReader.close();
            return new JSONObject(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    @SuppressWarnings("ConstantConditions")
    public static void printLog(String message) {
        if (ENVIRONMENT.equals("DEBUG")) {
            System.out.println(message);
        }
    }
}
