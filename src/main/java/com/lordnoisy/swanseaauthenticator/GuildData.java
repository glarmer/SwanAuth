package com.lordnoisy.swanseaauthenticator;

import discord4j.common.util.Snowflake;

/**
 * The GuildData class: Stores all needed data associated with a guild
 */
public class GuildData {
    private Snowflake verificationChannelID;
    private final Snowflake guildID;
    private Snowflake unverifiedRoleID;
    private Snowflake verifiedRoleID;
    private Snowflake adminChannelID;
    private String mode;

    /**
     * Constructor for guild data class
     *
     * @param guildID               the ID of the guild
     * @param verificationChannelID the channel ID for where the bot sends messages on join
     * @param adminChannelID        the admin channel, for sending logs
     * @param verifiedRoleID        the default role ID
     * @param unverifiedRoleID      the unverified role ID
     * @param verifiedRoleID        the verified role ID
     */
    public GuildData(String guildID, String adminChannelID, String verificationChannelID, String unverifiedRoleID, String verifiedRoleID, String mode) {
        this.guildID = Snowflake.of(guildID);
        if (verificationChannelID != null) {
            this.verificationChannelID = Snowflake.of(verificationChannelID);
        }
        if (adminChannelID != null) {
            this.adminChannelID = Snowflake.of(adminChannelID);
        }
        if (unverifiedRoleID != null) {
            this.unverifiedRoleID = Snowflake.of(unverifiedRoleID);
        } else {
            this.unverifiedRoleID = null;
        }
        if (verifiedRoleID != null) {
            this.verifiedRoleID = Snowflake.of(verifiedRoleID);
        }
        this.mode = mode;
    }

    /**
     * Return a new unconfigured GuildData object
     * @param guildID the ID of the guild
     * @return unconfigured GuildData
     */
    public static GuildData emptyGuildData(String guildID) {
        return new GuildData(guildID, null, null, null, null, "SLASH");
    }

    public Snowflake getVerificationChannelID() {
        return verificationChannelID;
    }

    public void setVerificationChannelID(Snowflake verificationChannelID) {
        this.verificationChannelID = verificationChannelID;
    }

    public Snowflake getGuildID() {
        return guildID;
    }

    public Snowflake getUnverifiedRoleID() {
        return unverifiedRoleID;
    }

    public void setUnverifiedRoleID(Snowflake unverifiedRoleID) {
        this.unverifiedRoleID = unverifiedRoleID;
    }

    public Snowflake getVerifiedRoleID() {
        return verifiedRoleID;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setVerifiedRoleID(Snowflake verifiedRoleID) {
        this.verifiedRoleID = verifiedRoleID;
    }

    public Snowflake getAdminChannelID() {
        return adminChannelID;
    }

    public void setAdminChannelID(Snowflake adminChannelID) {
        this.adminChannelID = adminChannelID;
    }
}
