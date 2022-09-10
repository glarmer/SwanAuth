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
    final private String CREATE_GUILDS_TABLE_SQL = "CREATE TABLE guilds (guild_id varchar(255) NOT NULL," +
            "admin_channel_id varchar(255)," +
            "verification_channel_id varchar(255)," +
            "unverified_role_id varchar(255)," +
            "verified_role_id varchar(255)," +
            "PRIMARY KEY (guild_id));";



    final private Connection CONNECTION;

    public SQLRunner (Connection connection){
        this.CONNECTION = connection;
    }

    public boolean createDatabase() {


        return true;
    }

    /**
     *
     * @param connection
     * @param discordID the discord ID to check exists
     * @param studentID the studentID to check exists
     * @return true if exists
     * @throws SQLException
     */
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

    /**
     *
     * @param connection the db connection
     * @param discordID the discordID to insert
     * @param studentID the studentID to insert
     * @throws SQLException
     */
    public static void createUser(Connection connection, String discordID, String studentID) throws SQLException {
        //Create query
        PreparedStatement finalQuery = connection.prepareStatement(insertUser);
        finalQuery.setString(1, discordID);
        finalQuery.setString(2, studentID);

        finalQuery.execute();
    }

    /**
     *
     * @param connection the db connection
     * @param discordID the discordID to select
     * @param studentID the studentID to select
     * @return
     * @throws SQLException
     */


}
