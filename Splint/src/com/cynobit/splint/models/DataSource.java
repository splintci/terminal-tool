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

    public int getPackageVersionId(String identifier) {
        try {
            PreparedStatement statement = connection.prepareStatement(String.format("SELECT * FROM %s WHERE %s = ?;",
                    Packages.TABLE_NAME, DBContract.getName(Packages.IDENTIFIER)));
            statement.setString(1, identifier);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(DBContract.getName(Packages.VERSION_ID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
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
