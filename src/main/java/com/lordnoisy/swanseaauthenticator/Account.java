package com.lordnoisy.swanseaauthenticator;

public record Account(String accountID, String userID, String discordID) {

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
