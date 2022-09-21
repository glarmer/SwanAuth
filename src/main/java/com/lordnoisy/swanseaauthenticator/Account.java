package com.lordnoisy.swanseaauthenticator;

public class Account {
    private String accountID;
    private String userID;
    private String discordID;

    public Account (String accountID, String userID, String discordID) {
        this.accountID = accountID;
        this.userID = userID;
        this.discordID = discordID;
    }

    public String getAccountID() {
        return accountID;
    }

    public String getUserID() {
        return userID;
    }

    public String getDiscordID() {
        return discordID;
    }
}
