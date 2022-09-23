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
    public GuildData(String guildID, String adminChannelID, String verificationChannelID, String unverifiedRoleID, String verifiedRoleID) {
        this.guildID = Snowflake.of(guildID);
        if (verificationChannelID != null) {
            this.verificationChannelID = Snowflake.of(verificationChannelID);
        }
        if (adminChannelID != null) {
            this.adminChannelID = Snowflake.of(adminChannelID);
        }
        if (unverifiedRoleID != null) {
            this.unverifiedRoleID = Snowflake.of(unverifiedRoleID);
        }
        if (verifiedRoleID != null) {
            this.verifiedRoleID = Snowflake.of(verifiedRoleID);
        }
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
