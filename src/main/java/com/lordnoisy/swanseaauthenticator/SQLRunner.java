package com.lordnoisy.swanseaauthenticator;

import discord4j.common.util.Snowflake;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;

public class SQLRunner {
    //Database + Table creation SQL
    final private String CREATE_DATABASE_SQL = "CREATE DATABASE AUTHENTICATOR;";
    final private String CREATE_GUILDS_TABLE_SQL = "CREATE TABLE guilds (" + "guild_id varchar(255) NOT NULL," + "admin_channel_id varchar(255)," + "verification_channel_id varchar(255)," + "unverified_role_id varchar(255)," + "verified_role_id varchar(255)," + "PRIMARY KEY (guild_id));";
    final private String CREATE_USERS_TABLE_SQL = "CREATE TABLE users(" + "user_id int NOT NULL AUTO_INCREMENT," + "student_id varchar(255)," + "PRIMARY KEY(user_id));";
    final private String CREATE_ACCOUNTS_TABLE_SQL = "CREATE TABLE accounts(" + "account_id int NOT NULL AUTO_INCREMENT," + "user_id int NOT NULL," + "discord_id varchar(255)," + "FOREIGN KEY (user_id) REFERENCES users(user_id)," + "PRIMARY KEY(account_id));";
    final private String CREATE_VERIFICATIONS_TABLE_SQL = "CREATE TABLE verifications(" + "account_id int NOT NULL," + "guild_id varchar(255) NOT NULL," + "FOREIGN KEY (guild_id) REFERENCES guilds(guild_id)," + "FOREIGN KEY (account_id) REFERENCES accounts(account_id));";
    final private String CREATE_BANS_TABLE_SQL = "CREATE TABLE bans(" + "user_id int NOT NULL," + "guild_id varchar(255) NOT NULL," + "FOREIGN KEY (user_id) REFERENCES users(user_id)," + "FOREIGN KEY (guild_id) REFERENCES guilds(guild_id));";
    final private String CREATE_VERIFICATION_TOKENS_TABLE = "CREATE TABLE verification_tokens(" + "account_id int NOT NULL," + "guild_id varchar(255) NOT NULL," + "token varchar(20)," + "timestamp datetime DEFAULT CURRENT_TIMESTAMP()," + "FOREIGN KEY (account_id) REFERENCES accounts(account_id));" + "FOREIGN KEY (guild_id) REFERENCES guilds(guild_id));";

    //SELECT Statements
    final private String SELECT_GUILD_SQL = "SELECT * FROM guilds WHERE guild_id = ?;";
    final private String SELECT_GUILDS_SQL = "SELECT * FROM guilds;";
    final private String SELECT_RECENT_VERIFICATION_TOKENS_SQL = "SELECT * FROM verification_tokens WHERE account_id = ? AND guild_id = ? AND timestamp > now() - interval 12 hour;";
    final private String SELECT_USER_BY_STUDENT_ID_SQL = "SELECT * FROM users WHERE student_id = ?;";
    final private String SELECT_ACCOUNT_BY_USER_AND_DISCORD_SQL = "SELECT * FROM accounts WHERE user_id = ? AND discord_id = ?;";
    final private String SELECT_VERIFICATION_TOKEN_SQL = "SELECT * FROM verification_tokens WHERE account_id = ? AND guild_id = ? AND token = ?;";
    final private String SELECT_ACCOUNT_BY_DISCORD_SQL = "SELECT * FROM accounts WHERE discord_id = ?;";
    final private String SELECT_VERIFIED_SQL = "SELECT * FROM verifications WHERE account_id = ? AND guild_id = ?;";
    final private String SELECT_VERIFIED_ANYWHERE_SQL = "SELECT * FROM verifications WHERE account_id = ?;";
    final private String SELECT_ACCOUNTS_BY_USER_ID_SQL = "SELECT * FROM accounts WHERE user_id = ?;";
    final private String SELECT_BANNED_SQL = "SELECT * FROM bans WHERE user_id = ? AND guild_id = ?;";
    final private String SELECT_STUDENT_ID_BY_USER_SQL = "SELECT * FROM users WHERE user_id = ?;";

    //INSERT Statements
    final private String INSERT_GUILD_SQL = "INSERT INTO guilds (guild_id) VALUES (?);";
    final private String INSERT_VERIFICATION_TOKEN_SQL = "INSERT INTO verification_tokens (account_id, guild_id, token) VALUES (?, ?, ?);";
    final private String INSERT_USER_SQL = "INSERT INTO users (student_id) VALUES (?);";
    final private String INSERT_ACCOUNT_SQL = "INSERT INTO accounts (user_id, discord_id) VALUES (?,?);";
    final private String INSERT_BAN_SQL = "INSERT INTO bans (user_id, guild_id) VALUES (?,?);";
    final private String INSERT_VERIFICATION_SQL = "INSERT INTO verifications (account_id, guild_id) VALUES (?,?);";


    //Update Statements
    final private String UPDATE_GUILD_DATA_SQL = "UPDATE guilds SET admin_channel_id = ?, verification_channel_id = ?, unverified_role_id = ?, verified_role_id = ? WHERE guild_id = ?;";
    final private String UPDATE_ACCOUNT_SQL = "UPDATE accounts SET user_id = ? WHERE account_id = ?;";

    //Delete Statements
    final private String DELETE_VERIFICATION_TOKENS_SQL = "DELETE FROM verification_tokens WHERE account_id = ? AND guild_id = ?;";
    final private String DELETE_ALL_VERIFICATION_TOKENS_SQL = "DELETE FROM verification_tokens WHERE account_id = ?;";
    final private String DELETE_BAN_SQL = "DELETE FROM bans WHERE user_id = ? AND guild_id = ?;";

    final private DataSource DATASOURCE;

    public SQLRunner(DataSource dataSource) {
        this.DATASOURCE = dataSource;

    }

    /**
     * Perform first time set up of the database
     *
     * @return true if successful, false otherwise
     */
    public boolean firstTimeSetup() {
        return (createGuildsTable() && createUsersTable() && createAccountsTable() && createVerificationsTable() && createBansTable() && createVerificationTokensTable());
    }

    public void databaseConfiguredCheck() {
        try (Connection connection = DATASOURCE.getDatabaseConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tableCheck = metaData.getTables(null, null, "guilds", null)) {
                if (tableCheck.next()) {
                    System.out.println("Database seems to exist... continuing.");
                } else {
                    if (!this.firstTimeSetup()) {
                        System.exit(1);
                    }
                    tableCheck.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Check if a guild already has an entry in the db
     * @param guildID the guild to check
     * @return true if it exists, false otherwise
     */
    public boolean dbHasGuild (String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(guildID);
        return isInTable(parameters, SELECT_GUILD_SQL);
    }

    /**
     * Delete all outstanding verification tokens for an account for a guild
     *
     * @param accountID account ID
     * @return true if successful, false otherwise
     */
    public boolean deleteVerificationTokens(String accountID, String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(accountID);
        parameters.add(guildID);
        return executeQuery(parameters, DELETE_VERIFICATION_TOKENS_SQL);
    }

    /**
     * Delete all outstanding verification tokens for an account
     *
     * @param accountID account ID
     * @return true if successful, false otherwise
     */
    public boolean deleteAllVerificationTokens(String accountID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(accountID);
        return executeQuery(parameters, DELETE_ALL_VERIFICATION_TOKENS_SQL);
    }

    /**
     * Delete a ban from the database
     *
     * @param userID  the user to unban
     * @param guildID the guild to unban them from
     * @return true on success, false otherwise
     */
    public boolean deleteBan(String userID, String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(userID);
        parameters.add(guildID);
        return executeQuery(parameters, DELETE_BAN_SQL);
    }

    /**
     * Create and/or get a userID using studentID
     *
     * @param studentNumber the users student number
     * @return the user id, null if unsuccessful
     */
    public String getOrCreateUserIDFromStudentID(String studentNumber) {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_STUDENT_ID_SQL)) {
            statement.setString(1, studentNumber);
            try (ResultSet userResults = statement.executeQuery()) {
                String userID;
                //Check if a user already exists for this student, insert otherwise, and get their ID
                if (!userResults.next()) {
                    //There are no rows, so we need to create a user
                    userResults.close();
                    this.insertUser(studentNumber);

                    //Run the method again to get the user_id of the freshly created user (I hate this)
                    userID = this.getOrCreateUserIDFromStudentID(studentNumber);
                } else {
                    userID = userResults.getString("user_id");
                }
                return userID;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getStudentIDFromUserID(String userID) {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_STUDENT_ID_BY_USER_SQL)) {
            statement.setString(1, userID);
            try (ResultSet accountResults = statement.executeQuery()) {
                accountResults.next();
                String studentID = accountResults.getString("student_id");

                return studentID;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            //forgive me for I have sinned
            return "null";
        }
    }

    /**
     * Get an account ID using both a discordID and a userID, if there isn't an account already - create one.
     *
     * @param userID    the users id
     * @param discordID the discord account id
     * @return the account id, null if unsuccessful
     */
    public String getOrCreateAccountIDFromDiscordIDAndUserID(String userID, String discordID) {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_ACCOUNT_BY_USER_AND_DISCORD_SQL)) {
            statement.setString(1, userID);
            statement.setString(2, discordID);

            try (ResultSet accountResults = statement.executeQuery()) {
                String accountID;
                //Check if a user already exists for this student, insert otherwise, and get their ID
                if (!accountResults.next()) {
                    //There are no rows, so we need to create a user
                    accountResults.close();
                    this.insertAccount(userID, discordID);

                    //Run the query again to get the account_id
                    accountID = getOrCreateAccountIDFromDiscordIDAndUserID(userID, discordID);
                } else {
                    accountID = accountResults.getString("account_id");
                }
                return accountID;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get an account ID solely from a Discord ID
     *
     * @param discordID the discord account id
     * @return the account id, null if unsuccessful
     */
    public Account getAccountFromDiscordID(String discordID) {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_ACCOUNT_BY_DISCORD_SQL)) {
            statement.setString(1, discordID);
            try (ResultSet accountResults = statement.executeQuery()) {
                accountResults.next();
                String accountID = accountResults.getString("account_id");
                String userID = accountResults.getString("user_id");

                return new Account(accountID, userID, discordID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets a list of accounts associated with a user
     *
     * @param userID the user id
     * @return the list of accounts
     */
    public ArrayList<Account> getAccountsFromUserID(String userID) {
        ArrayList<Account> accounts = new ArrayList<>();
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_ACCOUNTS_BY_USER_ID_SQL)) {
            statement.setString(1, userID);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String accountID = results.getString("account_id");
                    String discordID = results.getString("discord_id");

                    accounts.add(new Account(accountID, userID, discordID));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return accounts;
        }
        return accounts;
    }

    /**
     * Retrieves guilds from database and populates the given map
     *
     * @param guildDataMap the map to populate
     * @return true if successful, false if otherwise
     */
    public boolean populateGuildMapFromDatabase(Map<Snowflake, GuildData> guildDataMap) {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_GUILDS_SQL); ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                String currentGuildID = results.getString(1);
                String currentAdminChannelID = results.getString(2);
                String currentVerificationChannelID = results.getString(3);
                String currentUnverifiedRoleID = results.getString(4);
                String currentVerifiedRoleID = results.getString(5);
                GuildData guildData = new GuildData(currentGuildID, currentAdminChannelID, currentVerificationChannelID, currentUnverifiedRoleID, currentVerifiedRoleID);
                guildDataMap.put(Snowflake.of(currentGuildID), guildData);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves whether a row is found or not
     *
     * @param parameters a list of the parameters
     * @param sql        the sql query to run
     * @return true if exists, false otherwise
     */
    public boolean isInTable(ArrayList<String> parameters, String sql) {
        boolean exists = false;
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setString(i + 1, parameters.get(i));
            }
            try (ResultSet results = statement.executeQuery()) {
                int rows = 0;
                while (results.next()) {
                    rows += 1;
                }
                if (rows > 0) {
                    exists = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return exists;
        }
        return exists;
    }

    /**
     * Retrieves whether an account is verified
     *
     * @param userID  the account ID of the user
     * @param guildID the guild ID
     * @return true if banned, false otherwise
     */
    public boolean isBanned(String userID, String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(userID);
        parameters.add(guildID);
        return isInTable(parameters, SELECT_BANNED_SQL);
    }

    /**
     * Retrieves whether an account is verified
     *
     * @param accountID the account ID of the user
     * @return true if verified, false otherwise
     */
    public boolean isVerified(String accountID, String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(accountID);
        parameters.add(guildID);
        return isInTable(parameters, SELECT_VERIFIED_SQL);
    }

    /**
     * Retrieves whether an account is verified
     *
     * @param accountID the account ID of the user
     * @return true if verified, false otherwise
     */
    public boolean isVerifiedAnywhere(String accountID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(accountID);
        return isInTable(parameters, SELECT_VERIFIED_ANYWHERE_SQL);
    }

    /**
     * Select recent verification tokens of an account
     *
     * @param accountID the account to get associated tokens
     * @return the results of the query, -1 if failure
     */
    public int selectRecentVerificationTokens(String accountID, String guildID) {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_RECENT_VERIFICATION_TOKENS_SQL)) {
            statement.setString(1, accountID);
            statement.setString(2, guildID);
            try (ResultSet verificationTokens = statement.executeQuery()) {
                int rows = 0;
                while (verificationTokens.next()) {
                    rows += 1;
                }
                return rows;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Select verification token of an account
     *
     * @param accountID the account to get associated tokens
     * @param token     the token to select
     * @return the results of the query, -1 if failure
     */
    public int selectVerificationToken(String accountID, String guildID, String token) {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_VERIFICATION_TOKEN_SQL)) {
            statement.setString(1, accountID);
            statement.setString(2, guildID);
            statement.setString(3, token);
            try (ResultSet verificationTokens = statement.executeQuery()) {
                int rows = 0;
                while (verificationTokens.next()) {
                    rows += 1;
                }
                return rows;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Insert an account
     *
     * @param userID    the user id
     * @param discordID the discord account id
     * @return true if successful, false if otherwise
     */
    public boolean insertAccount(String userID, String discordID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(userID);
        parameters.add(discordID);
        return executeQuery(parameters, INSERT_ACCOUNT_SQL);
    }

    /**
     * Put a ban into the database
     *
     * @param userID  user to ban
     * @param guildID the guild to ban them from
     * @return
     */
    public boolean insertBan(String userID, String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(userID);
        parameters.add(guildID);
        return executeQuery(parameters, INSERT_BAN_SQL);
    }

    /**
     * Inserts a guild into the database
     *
     * @param guildID the id of the guild to insert
     * @return true if successful, false if otherwise
     */
    public boolean insertGuild(String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(guildID);
        return executeQuery(parameters, INSERT_GUILD_SQL);
    }

    /**
     * Insert a user
     *
     * @param studentID the user's student ID
     * @return true if successful, false if otherwise
     */
    public boolean insertUser(String studentID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(studentID);
        return executeQuery(parameters, INSERT_USER_SQL);
    }

    /**
     * Insert a verification
     *
     * @param accountID the accountID
     * @param guildID   the guildID
     * @return true if successful, false otherwise
     */
    public boolean insertVerification(String accountID, String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(accountID);
        parameters.add(guildID);
        return executeQuery(parameters, INSERT_VERIFICATION_SQL);
    }

    /**
     * Inserts a verification token into the db
     *
     * @param accountID         the account the token is associated with
     * @param verificationToken the verification token to insert
     * @return true if successful, false otherwise
     */
    public boolean insertVerificationToken(String accountID, String guildID, String verificationToken) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(accountID);
        parameters.add(guildID);
        parameters.add(verificationToken);
        return executeQuery(parameters, INSERT_VERIFICATION_TOKEN_SQL);
    }

    /**
     * Executes a non-resultset returning query
     *
     * @param parameters parameters to insert
     * @param insertSQL  the actual SQL
     * @return true on success, false otherwise
     */
    private boolean executeQuery(ArrayList<String> parameters, String insertSQL) {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setString(i + 1, parameters.get(i));
            }
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a guild's data into the db
     *
     * @param adminChannelID        admin channel ID
     * @param verificationChannelID verification channel ID
     * @param unverifiedRoleID      unverified role ID
     * @param verifiedRoleID        verified role ID
     * @param guildID               the guild ID
     * @return true if successful, false otherwise
     */
    public boolean updateGuildData(String adminChannelID, String verificationChannelID, String unverifiedRoleID, String verifiedRoleID, String guildID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(adminChannelID);
        parameters.add(verificationChannelID);
        parameters.add(unverifiedRoleID);
        parameters.add(verifiedRoleID);
        parameters.add(guildID);
        return executeQuery(parameters, UPDATE_GUILD_DATA_SQL);
    }

    /**
     * Updates an account
     *
     * @param userID        new user ID
     * @param accountID account ID to edit
     * @return true if successful, false otherwise
     */
    public boolean updateAccount(String userID, String accountID) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(userID);
        parameters.add(accountID);
        return executeQuery(parameters, UPDATE_ACCOUNT_SQL);
    }

    /**
     * Creates the guilds table
     *
     * @return true if successful, false otherwise
     */
    public boolean createGuildsTable() {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(CREATE_GUILDS_TABLE_SQL)) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates the users table
     *
     * @return true if successful, false otherwise
     */
    public boolean createUsersTable() {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(CREATE_USERS_TABLE_SQL)) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates the accounts table
     *
     * @return true if successful, false otherwise
     */
    public boolean createAccountsTable() {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(CREATE_ACCOUNTS_TABLE_SQL)) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates the verifications table
     *
     * @return true if successful, false otherwise
     */
    public boolean createVerificationsTable() {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(CREATE_VERIFICATIONS_TABLE_SQL)) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates the bans table
     *
     * @return true if successful, false otherwise
     */
    public boolean createBansTable() {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(CREATE_BANS_TABLE_SQL)) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates the verification_tokens table
     *
     * @return true if successful, false otherwise
     */
    public boolean createVerificationTokensTable() {
        try (Connection connection = DATASOURCE.getDatabaseConnection(); PreparedStatement statement = connection.prepareStatement(CREATE_VERIFICATION_TOKENS_TABLE)) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


}
