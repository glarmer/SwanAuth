package com.lordnoisy.swanseaauthenticator;

public class DiscordUtilities {
    public static final String MESSAGE_TOO_LONG = " ... This message was too long.";
    private static int MAX_MESSAGE_SIZE = 2000;

    public static String truncateMessage(String message) {
        if (message.length() > MAX_MESSAGE_SIZE) {
            message = message.substring(0, 1980).concat(MESSAGE_TOO_LONG);
        }
        return message;
    }

    public static String getMention(String memberID) {
        return "<@" + memberID + ">";
    }
}
