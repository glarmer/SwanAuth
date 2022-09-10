package com.lordnoisy.swanseaauthenticator;

import discord4j.common.util.Snowflake;

/**
 * The GuildData class: Stores all needed data associated with a guild
 */
public class GuildData {
    private Snowflake messageChannelID;
    private Snowflake guildID;
    private Snowflake unverifiedRoleID;
    private Snowflake verifiedRoleID;
    private Snowflake adminChannelID;
    private Snowflake defaultRoleID;

    /**
     * Constructor for guild data class
     * @param guildID the ID of the guild
     * @param messageChannelID the channel ID for where the bot sends messages on join
     * @param adminChannelID the admin channel, for sending logs
     * @param defaultRoleID the default role ID
     * @param unverifiedRoleID the unverified role ID
     * @param verifiedRoleID the verified role ID
     */
    public GuildData(String guildID, String messageChannelID, String adminChannelID, String defaultRoleID, String unverifiedRoleID, String verifiedRoleID){
        this.guildID = Snowflake.of(guildID);
        this.messageChannelID = Snowflake.of(messageChannelID);
        this.adminChannelID = Snowflake.of(adminChannelID);
        this.defaultRoleID = Snowflake.of(defaultRoleID);
        this.unverifiedRoleID = Snowflake.of(unverifiedRoleID);
        this.verifiedRoleID = Snowflake.of(verifiedRoleID);
    }

    public Snowflake getMessageChannelID() {
        return messageChannelID;
    }

    public Snowflake getGuildID() {
        return guildID;
    }

    public Snowflake getUnverifiedRoleID() {
        return unverifiedRoleID;
    }

    public Snowflake getVerifiedRoleID() {
        return verifiedRoleID;
    }

    public Snowflake getAdminChannelID() {
        return adminChannelID;
    }

    public Snowflake getDefaultRoleID() {
        return defaultRoleID;
    }
}
