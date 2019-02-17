package com.cynobit.splint.models;

import java.io.File;
import java.sql.*;

import com.cynobit.splint.models.DBContract.*;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class DataSource {

    private final static int DATABASE_VERSION = 1;

    private String dataBasePath;
    private String appRoot;
    private Connection connection;

    public DataSource(String appRoot) throws IllegalArgumentException {
        if (appRoot == null) throw new IllegalArgumentException("Null argument given.");
        if (appRoot.equals("")) throw new IllegalArgumentException("Null String given.");
        dataBasePath = appRoot + "cache.db";
        this.appRoot = appRoot;
    }

    public boolean isConnected() {
        return connection != null;
    }

    public boolean updatePackageInfo(String identifier, String version, int versionId, String integrity) {
        if (getPackageVersion(identifier).equals("z0.0.0")) {
            try {
                PreparedStatement statement = connection.prepareStatement(String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?);",
                        Packages.TABLE_NAME,
                        DBContract.getName(Packages.IDENTIFIER), DBContract.getName(Packages.VERSION),
                        DBContract.getName(Packages.VERSION_ID), DBContract.getName(Packages.INTEGRITY)));
                statement.setString(1, identifier);
                statement.setString(2, version);
                statement.setInt(3, versionId);
                statement.setString(4, integrity);
                statement.executeUpdate();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                PreparedStatement statement = connection.prepareStatement(String.format("UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ?;",
                        Packages.TABLE_NAME,
                        DBContract.getName(Packages.VERSION), DBContract.getName(Packages.VERSION_ID),
                        DBContract.getName(Packages.INTEGRITY), DBContract.getName(Packages.IDENTIFIER)));
                statement.setString(1, version);
                statement.setInt(2, versionId);
                statement.setString(3, integrity);
                statement.setString(4, identifier);
                statement.executeUpdate();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean connect() {
        boolean createTable = false;
        try {
            File dbFile = new File(dataBasePath);
            if (!dbFile.exists()) {
                createTable = true;
            }
            String url = "jdbc:sqlite:" + dataBasePath;
            connection = DriverManager.getConnection(url);
            if (createTable) {
                if (!createTables()) {
                    return false;
                }
            }
            if (new Preferences(appRoot).getDatabaseVersion() < DATABASE_VERSION) {
                if (!dropTables()) {
                    System.out.println("Error Code 0x001");
                    return false;
                }
                if (!createTables()) {
                    System.out.println("Error Code 0x002");
                    return false;
                }
                new Preferences(appRoot).setDatabaseVersion(DATABASE_VERSION).commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getPackageVersion(String identifier) {
        try {
            PreparedStatement statement = connection.prepareStatement(String.format("SELECT * FROM %s WHERE %s = ?;",
                    Packages.TABLE_NAME, DBContract.getName(Packages.IDENTIFIER)));
            statement.setString(1, identifier);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(DBContract.getName(Packages.VERSION));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "z0.0.0";
    }

    private boolean dropTables() {
        boolean success;
        try {
            Statement statement = connection.createStatement();
            success = statement.execute("DROP TABLE IF EXISTS " + Packages.TABLE_NAME);
            if (!success) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createTables() {
        try {
            Statement statement = connection.createStatement();
            // Packages Table.
            statement.execute(DBContract.createTable(Packages.TABLE_NAME, Packages.CONFIG, ""));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
