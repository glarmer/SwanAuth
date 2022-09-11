package com.lordnoisy.swanseaauthenticator;

import java.sql.*;

public class DatabaseConnector {
    private String url;
    private String user;
    private String password;
    private String database = "AUTHENTICATOR";
    private Connection connection;

    public DatabaseConnector (String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.connection = makeDatabaseConnection();
    }

    private Connection makeDatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Database connection established");
        } catch (Exception e) {
            // TODO: Need to write some actual code for this, error message and then close the bot, since if it can't
            // connect then there is no real point of the bot actually opening, right now this just stops it crashing
            // for no real reason.
            e.printStackTrace();
        }
        return connection;
    }

    public Connection getDatabaseConnection() {
        return connection;
    }


}