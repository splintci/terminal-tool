package com.cynobit.splint_update.models;

import com.cynobit.splint_update.Main;
import javafx.util.Pair;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;

//import java.net.HttpsURLConnection;

@SuppressWarnings("AnonymousHasLambdaAlternative")
public class CloudManager {

    public static final String USER_AGENT = "Splint Update";
    private static final String URL = "https://splint.cynobit.com/";
    //private static final String URL = "http://127.0.0.1/splint.cynobit.com/";
    public static final String BIN_API = URL + "index.php/Binaries/";
    private static final String CLIENT_API = URL + "index.php/SplintClient/";
    private static volatile CloudManager cloudManager;

    private CloudManager() {
        if (cloudManager != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static CloudManager getInstance() {
        if (cloudManager == null) {
            synchronized (CloudManager.class) {
                if (cloudManager == null) cloudManager = new CloudManager();
            }
        }
        return cloudManager;
    }

    public void getLatestDistributableVersion(CloudResponseListener listener) {
        fetch(BIN_API + "getLatestVersion", null, listener);
    }

    public void getLatestDistributionHash(CloudResponseListener listener) {
        fetch (BIN_API + "getUpdatePatchMD5", null,  listener);
    }

    @SuppressWarnings("unchecked")
    public void getLatestLoaderPatch(String sha1, CloudResponseListener listener) {
        ArrayList<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair("sha", sha1));
        ArrayList<String> headers = new ArrayList<>();
        headers.add("SHA-1");
        fetch(CLIENT_API + "getLatestLoaderPatch", parameters, headers, listener);
    }

    @SuppressWarnings("unchecked")
    public void getLatestURIPatch(String sha1, CloudResponseListener listener) {
        ArrayList<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair("sha", sha1));
        ArrayList<String> headers = new ArrayList<>();
        headers.add("SHA-1");
        fetch(CLIENT_API + "getLatestURIPatch", parameters, headers, listener);
    }

    private void fetch(String url, ArrayList<String> headers, final CloudResponseListener listener) {
        Thread httpThread = new Thread() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    URL obj = new URL(url);
                    HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

                    //add request header
                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", USER_AGENT);
                    con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                    int responseCode = con.getResponseCode();
                    Main.printLog("Sending 'GET' request to URL : " + url);
                    Main.printLog("Response Code : " + responseCode);

                    if (responseCode != 200) {
                        listener.onServerError(responseCode);
                        return;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Main.printLog(response.toString());
                    ArrayList<Pair<String, String>> returnHeaders = new ArrayList<>();
                    if (headers !=null) {
                        for (String key : headers) {
                            returnHeaders.add(new Pair(key, con.getHeaderField(key)));
                        }
                    }
                    listener.onResponseReceived(response.toString(), returnHeaders);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        httpThread.start();
    }

    @SuppressWarnings("AnonymousHasLambdaAlternative")
    private void fetch(String url, ArrayList<Pair<String, String>> parameters, ArrayList<String> headers, final CloudResponseListener listener) {
        Thread httpThread = new Thread() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    URL obj = new URL(url);
                    HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

                    //add request header
                    con.setRequestMethod("POST");
                    con.setRequestProperty("User-Agent", USER_AGENT);
                    con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                    StringBuilder parameterBuilder = new StringBuilder(parameters.get(0).getKey());
                    parameterBuilder.append("=").append(parameters.get(0).getValue());
                    for (int x = 1; x < parameters.size(); x++) {
                        parameterBuilder.append("&").append(parameters.get(x).getKey()).append("=");
                        parameterBuilder.append(parameters.get(x).getValue());
                    }

                    String urlParameters = parameterBuilder.toString();

                    // Send post request
                    con.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.writeBytes(urlParameters);
                    wr.flush();
                    wr.close();

                    int responseCode = con.getResponseCode();
                    Main.printLog("Sending 'POST' request to URL : " + url);
                    Main.printLog("Post parameters : " + urlParameters);
                    Main.printLog("Response Code : " + responseCode);

                    if (responseCode != 200) {
                        listener.onServerError(responseCode);
                        return;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Main.printLog(response.toString());
                    ArrayList<Pair<String, String>> returnHeaders = new ArrayList<>();
                    if (headers != null) {
                        for (String key : headers) {
                            returnHeaders.add(new Pair(key, con.getHeaderField(key)));
                        }
                    }
                    listener.onResponseReceived(response.toString(), returnHeaders);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onNetworkError();
                }
            }
        };
        httpThread.start();
    }

    public interface CloudResponseListener {
        void onResponseReceived(String response, ArrayList<Pair<String, String>> headers);

        void onServerError(int responseCode);

        void onNetworkError();
    }
}
