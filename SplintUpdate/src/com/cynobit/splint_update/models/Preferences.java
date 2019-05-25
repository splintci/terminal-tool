package com.cynobit.splint_update.models;

import org.json.JSONObject;

import java.io.*;

@SuppressWarnings("FieldCanBeLocal")
public class Preferences {

    private String preferencePath;
    private JSONObject preferences;

    /**
     * Keys
     */
    private static String SDK_VERSION = "splint_sdk_version";

    public Preferences(String appRoot) throws IllegalArgumentException {
        if (appRoot == null) throw new IllegalArgumentException("Null argument given.");
        if (appRoot.equals("")) throw new IllegalArgumentException("Null String given.");
        preferencePath = appRoot + "preferences.json";
        try {
            String line;
            StringBuilder builder = new StringBuilder();
            File prefFile = new File(preferencePath);
            if (!prefFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                prefFile.createNewFile();
                preferences = new JSONObject();
            } else {
                FileReader fileReader = new FileReader(prefFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                while ((line = bufferedReader.readLine()) != null) {
                    builder.append(line);
                }
                bufferedReader.close();
                if (builder.toString().contains("{") && builder.toString().contains("}")) {
                    preferences = new JSONObject(builder.toString());
                } else {
                    preferences = new JSONObject();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getSDKVersion() {
        return getValueFromJson(SDK_VERSION, "v0.0.0").toString();
    }

    public void setSDKVersion(String version) {
        putValueInJson(SDK_VERSION, version);
    }

    public void commit() {
        try {
            FileWriter fileWriter = new FileWriter(preferencePath);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(preferences.toString());
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object getValueFromJson(String key, Object defaultValue) {
        try {
            return preferences.has(key) ? preferences.get(key) : defaultValue;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    private void putValueInJson(String key, int value) {
        try {
            preferences.put(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void putValueInJson(String key, String value) {
        try {
            preferences.put(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
