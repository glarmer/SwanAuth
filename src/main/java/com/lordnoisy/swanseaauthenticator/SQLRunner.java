package com.lordnoisy.swanseaauthenticator;

import java.sql.*;

public class SQLRunner {
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
            "token varchar(20)," +
            "timestamp datetime DEFAULT CURRENT_TIMESTAMP()," +
            "FOREIGN KEY (account_id) REFERENCES accounts(account_id));";

    //SELECT Statements
    final private String SELECT_GUILDS_SQL = "SELECT * FROM guilds;";
    final private String SELECT_VERIFICATION_TOKENS_SQL = "SELECT * FROM verification_tokens WHERE account_id = ? AND timestamp > now() - interval 12 hour;";
    final private String SELECT_USER_BY_STUDENT_ID_SQL = "SELECT * FROM users WHERE student_id = ?;";
    final private String SELECT_ACCOUNT_BY_USER_AND_DISCORD_SQL = "SELECT * FROM accounts WHERE user_id = ? AND discord_id = ?;";

    //INSERT Statements
    final private String INSERT_GUILD_SQL = "INSERT INTO guilds (guild_id) VALUES (?);";
    final private String INSERT_VERIFICATION_TOKEN_SQL = "INSERT INTO verification_tokens (account_id, token) VALUES (?, ?);";
    final private String INSERT_USER_SQL = "INSERT INTO users (student_id) VALUES (?);";
    final private String INSERT_ACCOUNT_SQL = "INSERT INTO accounts (user_id, discord_id) VALUES (?,?);";

    //Update Statements
    final private String UPDATE_GUILD_DATA_SQL = "UPDATE guilds SET admin_channel_id = ?, verification_channel_id = ?, unverified_role_id = ?, verified_role_id = ? WHERE guild_id = ?;";

    final private Connection CONNECTION;

    public SQLRunner (Connection connection){
        this.CONNECTION = connection;
    }

    /**
     * Perform first time set up of the database
     */
    public void firstTimeSetup() {
        try {
            createGuildsTable();
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
     * Retrieves guilds from database
     * @return guilds
     * @throws SQLException if query fails
     */
    public ResultSet getGuilds() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(SELECT_GUILDS_SQL);
        return statement.executeQuery();
    }

    /**
     * Select a user by their student ID
     * @param studentID the user's student ID
     * @return the results of the query
     * @throws SQLException if query fails
     */
    public ResultSet selectUser(String studentID) throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(SELECT_USER_BY_STUDENT_ID_SQL);
        statement.setString(1, studentID);
        return statement.executeQuery();
    }

    public ResultSet selectAccount(String userID, String discordID) throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(SELECT_ACCOUNT_BY_USER_AND_DISCORD_SQL);
        statement.setString(1, userID);
        statement.setString(2, discordID);
        return statement.executeQuery();
    }

    /**
     * Select verification tokens of an account
     * @param accountID the account to get associated tokens
     * @return the results of the query
     * @throws SQLException if query fails
     */
    public ResultSet selectVerificationTokens(String accountID) throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(SELECT_VERIFICATION_TOKENS_SQL);
        statement.setString(1, accountID);
        return statement.executeQuery();
    }

    public void insertAccount(String userID, String discordID) throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(INSERT_ACCOUNT_SQL);
        statement.setString(1, userID);
        statement.setString(2, discordID);
        statement.execute();
        statement.close();
    }

    /**
     * Inserts a guild into the database
     * @param guildID the id of the guild to insert
     * @throws SQLException if query fails
     */
    public void insertGuild(String guildID) throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(INSERT_GUILD_SQL);
        statement.setString(1, guildID);
        statement.execute();
        statement.close();
    }

    public void insertUser(String studentID) throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(INSERT_USER_SQL);
        statement.setString(1, studentID);
        statement.execute();
        statement.close();
    }

    /**
     * Inserts a verification token into the db
     * @param accountID the account the token is associated with
     * @param verificationToken the verification token to insert
     */
    public void insertVerificationToken(String accountID, String verificationToken) throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(INSERT_VERIFICATION_TOKEN_SQL);
        statement.setString(1, accountID);
        statement.setString(2, verificationToken);
        statement.execute();
        statement.close();
    }

    /**
     * Inserts a guild's data into the db
     * @param adminChannelID admin channel ID
     * @param verificationChannelID verification channel ID
     * @param unverifiedRoleID unverified role ID
     * @param verifiedRoleID verified role ID
     * @param guildID the guild ID
     * @throws SQLException if query fails
     */
    public void updateGuildData(String adminChannelID, String verificationChannelID, String unverifiedRoleID, String verifiedRoleID, String guildID) throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(UPDATE_GUILD_DATA_SQL);
        statement.setString(1, adminChannelID);
        statement.setString(2, verificationChannelID);
        statement.setString(3, unverifiedRoleID);
        statement.setString(4, verifiedRoleID);
        statement.setString(5, guildID);
        statement.execute();
        statement.close();
    }

    /**
     * Creates the guilds table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createGuildsTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_GUILDS_TABLE_SQL);
        statement.execute();
        statement.close();
    }

    /**
     * Creates the users table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createUsersTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_USERS_TABLE_SQL);
        statement.execute();
        statement.close();
    }

    /**
     * Creates the accounts table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createAccountsTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_ACCOUNTS_TABLE_SQL);
        statement.execute();
        statement.close();
    }

    /**
     * Creates the verifications table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createVerificationsTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_VERIFICATIONS_TABLE_SQL);
        statement.execute();
        statement.close();
    }

    /**
     * Creates the bans table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createBansTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_BANS_TABLE_SQL);
        statement.execute();
        statement.close();
    }

    /**
     * Creates the verification_tokens table
     * @throws SQLException thrown if the table can't be created.
     */
    public void createVerificationTokensTable() throws SQLException {
        PreparedStatement statement = CONNECTION.prepareStatement(CREATE_VERIFICATION_TOKENS_TABLE);
        statement.execute();
        statement.close();
    }


}
