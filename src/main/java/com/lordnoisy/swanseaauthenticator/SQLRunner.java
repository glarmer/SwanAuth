package com.lordnoisy.swanseaauthenticator;

import java.sql.*;
import java.util.ArrayList;

public class SQLRunner {
    //TODO : Probably a lot of this will change because the backend needs to change to accommodate multiple servers

    final private static String deletePendingVerification = "DELETE FROM pending WHERE verification_key = ?;";
    final private static String insertUserIDIntoBlacklisted = "INSERT INTO blacklisted (user_id) VALUES (?);";
    final private static String selectPendingInvitesMadeWithin24Hours = "SELECT * FROM pending WHERE user_id = ? AND time_created >= NOW() - INTERVAL 1 DAY;";

    //Database + Table creation SQL
    final private String CREATE_DATABASE_SQL = "CREATE DATABASE AUTHENTICATOR;";
    final private String CREATE_GUILDS_TABLE_SQL = "CREATE TABLE guilds (" +
            "guild_id varchar(255) NOT NULL," +
            "admin_channel_id varchar(255)," +
            "verification_channel_id varchar(255)," +
            "unverified_role_id varchar(255)," +
            "verified_role_id varchar(255)," +
            "PRIMARY KEY (guild_id));";
    final private String CREATE_USERS_TABLE_SQL = "CREATE TABLE users(" +
            "user_id int NOT NULL AUTO_INCREMENT," +
            "student_id varchar(255)," +
            "PRIMARY KEY(user_id));";
    final private String CREATE_ACCOUNTS_TABLE_SQL = "CREATE TABLE accounts(" +
            "account_id int NOT NULL AUTO_INCREMENT," +
            "user_id int NOT NULL," +
            "discord_id varchar(255)," +
            "FOREIGN KEY (user_id) REFERENCES users(user_id)," +
            "PRIMARY KEY(account_id));";
    final private String CREATE_VERIFICATIONS_TABLE_SQL = "CREATE TABLE verifications(" +
            "account_id int NOT NULL," +
            "guild_id varchar(255) NOT NULL," +
            "FOREIGN KEY (guild_id) REFERENCES guilds(guild_id)," +
            "FOREIGN KEY (account_id) REFERENCES accounts(account_id));";
    final private String CREATE_BANS_TABLE_SQL = "CREATE TABLE bans(" +
            "user_id int NOT NULL," +
            "guild_id varchar(255) NOT NULL," +
            "FOREIGN KEY (user_id) REFERENCES users(user_id)," +
            "FOREIGN KEY (guild_id) REFERENCES guilds(guild_id));";
    final private String CREATE_VERIFICATION_TOKENS_TABLE = "CREATE TABLE verification_tokens(" +
            "account_id int NOT NULL," +
            "token varchar(255)," +
            "timestamp datetime DEFAULT CURRENT_TIMESTAMP()," +
            "FOREIGN KEY (account_id) REFERENCES accounts(account_id));";



    final private Connection CONNECTION;

    public SQLRunner (Connection connection){
        this.CONNECTION = connection;
    }

    public void firstTimeSetup() {
        try {
            createDatabase();
            createUsersTable();
            createAccountsTable();
            createVerificationsTable();
            createBansTable();
            createVerificationTokensTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the database
     * @throws SQLException thrown if database can't be created.
     */
    public void createDatabase() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_DATABASE_SQL);
        statement.executeQuery();
    }

    /**
     * Creates the guilds table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createGuildsTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_GUILDS_TABLE_SQL);
        statement.executeQuery();
    }

    /**
     * Creates the users table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createUsersTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_USERS_TABLE_SQL);
        statement.executeQuery();
    }

    /**
     * Creates the accounts table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createAccountsTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_ACCOUNTS_TABLE_SQL);
        statement.executeQuery();
    }

    /**
     * Creates the verifications table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createVerificationsTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_VERIFICATIONS_TABLE_SQL);
        statement.executeQuery();
    }

    /**
     * Creates the bans table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createBansTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_BANS_TABLE_SQL);
        statement.executeQuery();
    }

    /**
     * Creates the verification_tokens table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createVerificationTokensTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_VERIFICATION_TOKENS_TABLE);
        statement.executeQuery();
    }

    //TODO: Delete
    public static boolean checkIfUserExists(Connection connection, String discordID, String studentID) throws SQLException {
        boolean userExists = false;

        //Create query
        PreparedStatement finalQuery = connection.prepareStatement(selectUserQuery);
        finalQuery.setString(1, discordID);
        finalQuery.setString(2, studentID);

        //Execute it
        ResultSet resultSet = finalQuery.executeQuery();

        //Count the results
        int totalResults = 0;
        while (resultSet.next()) {
            totalResults ++;
        }

        //If there are results, return true
        if (totalResults > 0) {
            userExists = true;
        }
        return userExists;
    }


}
