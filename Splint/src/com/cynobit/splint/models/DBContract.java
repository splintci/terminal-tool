package com.cynobit.splint.models;

class DBContract {
    static class Packages {
        static String TABLE_NAME = "packages";
        static String ID = "id INTEGER AUTO_INCREMENT PRIMARY KEY";
        static String IDENTIFIER = "identifier TEXT UNIQUE NOT NULL";
        static String VERSION_ID = "version_id INTEGER NOT NULL";
        static String VERSION = "version TEXT";
        static String INTEGRITY = "integrity TEXT";
        static String[] CONFIG = {ID, IDENTIFIER, VERSION_ID, VERSION, INTEGRITY};
    }

    static String createTable(String name, String[] configs, String constraints) {
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(name).append(" (");
        for (String column : configs) {
            sql.append(column).append(", ");
        }
        if (!constraints.equals("")) {
            sql.append(constraints);
        } else {
            sql.replace(sql.lastIndexOf(","), sql.lastIndexOf(",") + 2, "");
        }
        sql.append(");");
        return sql.toString();
    }

    static String getName(String config) {
        return config.substring(0, config.indexOf(" "));
    }

}
