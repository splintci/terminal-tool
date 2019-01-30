package com.cynobit.splint.models;

import com.cynobit.splint.Main;
import javafx.util.Pair;

//import javax.net.ssl.HttpURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("AnonymousHasLambdaAlternative")
public class CloudManager {

    public static final String USER_AGENT = Main.APP_NAME;
    //private static final String URL = "https://splint.cynobit.com/";
    private static final String URL = "http://127.0.0.1/splint.cynobit.com/";
    public static final String API = URL + "index.php/SplintClient/";
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

    public void getLatestVersion(String identifier, CloudResponseListener listener) {
        String request = API + "getLatestVersionId";
        ArrayList<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>("identifier", identifier));
        fetch(request, parameters, listener);
    }

    public void requestPackages(List<String> packages, CloudResponseListener listener) {
        String request = API + "requestPackages";
        ArrayList<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>("identifiers", String.join(",", packages)));
        fetch(request, parameters, listener);
    }

    private void fetch(String url, final CloudResponseListener listener) {
        Thread httpThread = new Thread() {
            @Override
            public void run() {
                try {
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

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
                    listener.onResponseReceived(response.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        httpThread.start();
    }

    @SuppressWarnings("AnonymousHasLambdaAlternative")
    private void fetch(String url, ArrayList<Pair<String, String>> parameters, final CloudResponseListener listener) {
        Thread httpThread = new Thread() {
            @Override
            public void run() {
                try {
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

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
                    listener.onResponseReceived(response.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onNetworkError();
                }
            }
        };
        httpThread.start();
    }

    @SuppressWarnings("SameParameterValue")
    private void multiPartFetch(String url, ArrayList<MultiPartParameter> parameters, CloudResponseListener listener) {
        Thread multiPartHttp = new Thread() {
            @Override
            public void run() {
                try {
                    URL obj = new URL(url);
                    HttpURLConnection urlConnection = (HttpURLConnection) obj.openConnection();
                    String boundaryString = "----SSVPDataBase";

                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundaryString);

                    OutputStream outputStreamToRequestBody = urlConnection.getOutputStream();
                    BufferedWriter httpRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(outputStreamToRequestBody));

                    httpRequestBodyWriter.write("\n\n--" + boundaryString + "\n");

                    for (MultiPartParameter parameter : parameters) {
                        if (parameter.getType() == PARAMETER_TYPE.STRING) {
                            httpRequestBodyWriter.write("Content-Disposition: form-data;name=\"" + parameter.getName() + "\"");
                            httpRequestBodyWriter.write("\n\n");
                            httpRequestBodyWriter.write(parameter.getValue());
                        } else if (parameter.getType() == PARAMETER_TYPE.IMAGE) {
                            httpRequestBodyWriter.write("Content-Disposition: form-data;name=\"" +
                                    parameter.getName() + "\";" + "filename=\"" +
                                    parameter.getFileName() + "\"" + "\nContent-Type: " + extensionToMimeType(parameter.getFileName()) + "\n\n");
                            httpRequestBodyWriter.flush();
                            FileInputStream fileInputStream = parameter.getFileInputStream();
                            int bytesRead;
                            byte[] dataBuffer = new byte[1024];
                            while ((bytesRead = fileInputStream.read(dataBuffer)) != -1) {
                                outputStreamToRequestBody.write(dataBuffer, 0, bytesRead);
                            }
                            outputStreamToRequestBody.flush();
                        }
                        httpRequestBodyWriter.write("\n--" + boundaryString + "\n");
                    }
                    httpRequestBodyWriter.write("\n--" + boundaryString + "--\n");
                    httpRequestBodyWriter.flush();
                    outputStreamToRequestBody.close();
                    httpRequestBodyWriter.close();
                    BufferedReader httpResponseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String lineRead;
                    StringBuilder builder = new StringBuilder();
                    while ((lineRead = httpResponseReader.readLine()) != null) {
                        builder.append(lineRead);
                    }
                    listener.onResponseReceived(builder.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        multiPartHttp.start();
    }

    @SuppressWarnings("ConstantConditions")

    public interface CloudResponseListener {
        void onResponseReceived(String response);

        void onServerError(int responseCode);

        void onNetworkError();
    }

    private String extensionToMimeType(String fileName) {
        switch (fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            default:
                return "application/octet-stream";
        }
    }

    class MultiPartParameter {
        PARAMETER_TYPE type = PARAMETER_TYPE.STRING;
        String name;
        String value;
        String fileName;
        File file;

        MultiPartParameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        MultiPartParameter(String name, int value) {
            this.name = name;
            this.value = String.valueOf(value);
        }

        MultiPartParameter(String name, File file, PARAMETER_TYPE type) {
            this.name = name;
            this.type = type;
            this.file = file;
            fileName = file.getName();
        }

        FileInputStream getFileInputStream() {
            try {
                return new FileInputStream(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        PARAMETER_TYPE getType() {
            return type;
        }

        String getName() {
            return name;
        }

        String getValue() {
            return value;
        }

        String getFileName() {
            return fileName;
        }
    }

    enum PARAMETER_TYPE {
        STRING("string"), IMAGE("image");

        String value;

        PARAMETER_TYPE(String value) {
            this.value = value;
        }
    }

}
