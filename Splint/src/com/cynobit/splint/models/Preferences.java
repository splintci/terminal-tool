package com.cynobit.splint.models;

import org.json.JSONObject;

import java.io.*;

@SuppressWarnings("FieldCanBeLocal")
public class Preferences {

    private String preferencePath;
    private JSONObject preferences;

    /**
     * Keys
     */
    private static String DB_VERSION = "db_version";
    private static String FMD_BATCH = "fmd_batch";

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

    int getDatabaseVersion() {
        return Integer.valueOf(String.valueOf(getValueFromJson(DB_VERSION, 1)));
    }

    public int getFMDBatch() {
        return Integer.valueOf(String.valueOf(getValueFromJson(FMD_BATCH, 0)));
    }

    @SuppressWarnings("SameParameterValue")
    Preferences setDatabaseVersion(int dbVersion) {
        putValueInJson(DB_VERSION, dbVersion);
        return this;
    }

    void commit() {
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

}
