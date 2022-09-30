package com.lordnoisy.swanseaauthenticator;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.UnbanEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.*;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    //Constants are sorted into groups, and in A-Z order within those groups

    //Command Names
    public static final String BEGIN_COMMAND_NAME = "begin";
    public static final String HELP_COMMAND_NAME = "help";
    public static final String NON_STUDENT_VERIFY_COMMAND_NAME = "nonstudentverify";
    public static final String SETUP_COMMAND_NAME = "setup";
    public static final String VERIFY_COMMAND_NAME = "verify";

    //Command Descriptions
    public static final String BEGIN_COMMAND_DESCRIPTION = "Begin the verification process by entering your Student ID";
    public static final String HELP_COMMAND_DESCRIPTION = "Run this command to get help on how to use the bot!";
    public static final String NON_STUDENT_VERIFY_COMMAND_DESCRIPTION = "Use this to ask for verification if you do not have a student number";
    public static final String SETUP_COMMAND_DESCRIPTION = "Configure the bot so it can begin verifying users";
    public static final String VERIFY_COMMAND_DESCRIPTION = "Finish verifying by entering your verification token :)";

    //Command Option Names
    public static final String ADMIN_CHANNEL_OPTION = "admin_channel";
    public static final String APPLY_UNVERIFIED_OPTION = "apply_unverified_role";
    public static final String REASON_OPTION = "reason";
    public static final String STUDENT_ID_OPTION = "student_id";
    public static final String UNVERIFIED_ROLE_OPTION = "unverified_role";
    public static final String VERIFICATION_CHANNEL_OPTION = "verification_channel";
    public static final String VERIFICATION_CODE_OPTION = "verification_code";
    public static final String VERIFIED_ROLE_OPTION = "verified_role";

    //Command Option Descriptions
    public static final String ADMIN_CHANNEL_OPTION_DESCRIPTION = "The channel that you want the bot to use for admin messages.";
    public static final String REASON_OPTION_DESCRIPTION = "The reason why you need to manually verify, e.g. you are a staff member...";
    public static final String STUDENT_ID_OPTION_DESCRIPTION = "Your Student ID";
    public static final String UNVERIFIED_ROLE_OPTION_DESCRIPTION = "The unverified role you want to apply to users when they join the server.";
    public static final String VERIFICATION_CHANNEL_OPTION_DESCRIPTION = "The channel that you want the bot to use for verifications.";
    public static final String VERIFICATION_CODE_OPTION_DESCRIPTION = "The verification code you received via email!";
    public static final String VERIFIED_ROLE_OPTION_DESCRIPTION = "The verified role you want to apply to users after they verify.";

    //Results
    public static final String BEGIN_COMMAND_SUCCESS_RESULT = "Please check your student email for a verification code, make sure to check your spam as it might've been sent there. Once you have your code, use /verify to finish verifying.";
    public static final String BEGIN_COMMAND_SUCCESS_RESULT_MODAL = "Please check your student email for a verification code, make sure to check your spam as it might've been sent there. Once you have your code, click the button below to finish verifying!";
    public static final String USER_IS_BANNED_RESULT = "This user has been banned on a different Discord account, and so is no longer allowed on this server...";

    //Success Messages
    public static final String HELP_COMMAND_SUCCESS = "To begin verification run /verify! Admins can run /setup to configure the bot!";
    public static final String MANUAL_VERIFICATION_COMMAND_SUCCESS = "Your manual verification request has been sent to the admins!";
    public static final String SETUP_COMMAND_SUCCESS = "You have successfully configured the bot!";
    public static final String VERIFY_COMMAND_SUCCESS = "You have successfully verified!";

    //Errors
    public static final String ACCOUNT_ALREADY_VERIFIED_ERROR = "This discord account is already verified on this server!";
    public static final String APPLY_UNVERIFIED_OPTION_DESCRIPTION = "Auto apply the unverified role to all users who don't already have it or the verified role.";
    public static final String DATABASE_ERROR = "There was an error contacting the database, please try again or contact the bot admin for help";
    public static final String DEFAULT_ERROR = "An error has occurred, please try again or contact an admin for help";
    public static final String HAVE_NOT_BEGUN_ERROR = "You need to use the /begin command before trying to use /verify!";
    public static final String INCORRECT_COMMANDLINE_ARGUMENTS_ERROR = "You have entered the incorrect amount of command line arguments, please check that your mySQL and Email login is entered correctly.";
    public static final String INCORRECT_STUDENT_NUMBER_ERROR = "The student number you entered was incorrect, please try again! If you do not have a student number (e.g. if you are a staff member or alumni) please request verification with /nonstudentverify!";
    public static final String INCORRECT_TOKEN_ERROR = "The verification token you entered is incorrect, please try again...";
    public static final String INSUFFICIENT_PERMISSIONS_ERROR = "You don't have permissions to perform this command! You need to be an administrator on the server to do this.";
    public static final String SERVER_NOT_CONFIGURED_ERROR = "The server admins haven't configured the bot yet, contact them for assistance.";
    public static final String SETUP_COMMAND_ERROR = "Configuring bot failed, please try again or contact the bot admin.";
    public static final String TOO_MANY_ATTEMPTS_ERROR = "You have made too many attempts to begin verification recently, please either verify using an existing token or try again later.";
    public static final String TOO_MANY_VERIFICATION_REQUESTS_ERROR = "You already have a pending verification request! Please wait a while...";
    public static final String VERIFIED_UNDER_ANOTHER_STUDENT_ID_ERROR = "You have already verified with another student ID previously!";

    //Misc
    public static final Color EMBED_COLOUR = Color.of(89, 82, 255);
    public static final String FOOTER_ICON_URL = "https://media.discordapp.net/attachments/1020458334882631690/1022266062579974184/SwanAuth.png?width=910&height=910";
    public static final Integer MAX_VERIFICATION_REQUESTS = 1;
    public static final String WAS_BANNED = " was banned";
    public static final String WAS_UNBANNED = " was unbanned";

    //Buttons
    public static final String BUTTON_ACCEPT = "accept";
    public static final String BUTTON_DENY = "deny";
    public static final String BUTTON_ID = "swanauth";
    public static final String EMAIL_ERROR = "There was an error sending the email, please try again or contact an admin for help";
    public static final String BUTTON_MODE_OPTION = "button_mode";
    public static final String BUTTON_MODE_OPTION_DESCRIPTION = "Select if users should be prompted to use a button instead of a slash command.";
    public static final String BUTTON_VERIFY = "verify";
    public static final String MODAL_ID = "swanauthmodal";
    public static final String INPUT_ID = "swanauthinput";
    public static final String INPUT_STUDENT = "studentnumber";
    public static final String BUTTON_FINISH_VERIFICATION = "finish_verification";
    public static final String MODAL_FINISH_ID = "swanauthfinishmodal";
    public static final String INPUT_CODE = "verificationcode";

    //0 Token 1 MYSQL URL 2 MYSQL Username 3 MYSQL password 4 Email Host 5 Email port 6 Email username 7 Email password 8 Sender Email Address
    public static void main(String[] args) {
        if (args.length == 9) {
            String token = args[0];
            DataSource databaseConnector = new DataSource(args[1], args[2], args[3]);
            SQLRunner sqlRunner = new SQLRunner(databaseConnector);

            String host = args[4];
            int port = Integer.parseInt(args[5]);
            String username = args[6];
            String password = args[7];
            String senderEmail = args[8];
            EmailSender emailSender = new EmailSender(host, port, username, password, senderEmail);
            sqlRunner.databaseConfiguredCheck();

            // Creates a map containing details of each server stored in the mySQL db
            final Map<Snowflake, GuildData> guildDataMap = new HashMap<>();
            final Map<String, Integer> manualVerificationsMap = new HashMap<>();

            sqlRunner.populateGuildMapFromDatabase(guildDataMap);

            DiscordClient client = DiscordClient.create(token);
            IntentSet intents = IntentSet.of(Intent.GUILD_BANS, Intent.GUILD_MEMBERS);
            Mono<Void> login = client.gateway().setEnabledIntents(intents).withGateway((GatewayDiscordClient gateway) -> {

                // Make commands
                ApplicationCommandRequest beginCommand = ApplicationCommandRequest.builder()
                        .name(BEGIN_COMMAND_NAME)
                        .description(BEGIN_COMMAND_DESCRIPTION)
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(STUDENT_ID_OPTION)
                                .description(STUDENT_ID_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .maxLength(255)
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest verifyCommand = ApplicationCommandRequest.builder()
                        .name(VERIFY_COMMAND_NAME)
                        .description(VERIFY_COMMAND_DESCRIPTION)
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(VERIFICATION_CODE_OPTION)
                                .description(VERIFICATION_CODE_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .maxLength(255)
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest nonStudentVerifyCommand = ApplicationCommandRequest.builder()
                        .name(NON_STUDENT_VERIFY_COMMAND_NAME)
                        .description(NON_STUDENT_VERIFY_COMMAND_DESCRIPTION)
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(REASON_OPTION)
                                .description(REASON_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .maxLength(500)
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest setupCommand = ApplicationCommandRequest.builder()
                        .name(SETUP_COMMAND_NAME)
                        .description(SETUP_COMMAND_DESCRIPTION)
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(VERIFICATION_CHANNEL_OPTION)
                                .description(VERIFICATION_CHANNEL_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                                .channelTypes(List.of(0))
                                .maxLength(255)
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(ADMIN_CHANNEL_OPTION)
                                .description(ADMIN_CHANNEL_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                                .channelTypes(List.of(0))
                                .maxLength(255)
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(UNVERIFIED_ROLE_OPTION)
                                .description(UNVERIFIED_ROLE_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.ROLE.getValue())
                                .maxLength(255)
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(VERIFIED_ROLE_OPTION)
                                .description(VERIFIED_ROLE_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.ROLE.getValue())
                                .maxLength(255)
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(BUTTON_MODE_OPTION)
                                .description(BUTTON_MODE_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name(APPLY_UNVERIFIED_OPTION)
                                .description(APPLY_UNVERIFIED_OPTION_DESCRIPTION)
                                .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest helpCommand = ApplicationCommandRequest.builder()
                        .name(HELP_COMMAND_NAME)
                        .description(HELP_COMMAND_DESCRIPTION)
                        .build();

                List<ApplicationCommandRequest> applicationCommandRequestList = List.of(beginCommand, verifyCommand, helpCommand, setupCommand, nonStudentVerifyCommand);


                Mono<Void> doOnEachGuild = gateway.getGuilds().doOnEach(guild -> {
                    try {
                        Snowflake guildID = guild.get().getId();
                        // Check for guilds that are present but not in the map, and thus not in the db. This could happen if they invite the bot when it's offline
                        if (guildDataMap.get(guildID) == null) {
                            sqlRunner.insertGuild(guildID.asString());
                            GuildData guildData = GuildData.emptyGuildData(guildID.asString());
                            guildDataMap.put(guildID, guildData);
                        }
                    } catch (NullPointerException npe) {
                        System.out.println("Continuing");
                    }
                }).then();

                Mono<Void> createGlobalApplicationCommands = DiscordUtilities.createGlobalCommandsMono(applicationCommandRequestList, gateway);

                Mono<Void> actOnJoin = gateway.on(MemberJoinEvent.class, event -> {
                    Snowflake serverID = event.getGuildId();
                    GuildData guildData = guildDataMap.get(serverID);
                    Snowflake unverifiedRoleID = guildData.getUnverifiedRoleID();
                    Snowflake verifiedRoleID = guildData.getVerifiedRoleID();
                    Snowflake verificationChannelID = guildData.getVerificationChannelID();
                    Snowflake adminChannelID = guildData.getAdminChannelID();
                    String mode = guildData.getMode();

                    if (unverifiedRoleID == null || verificationChannelID == null) {
                        return Mono.empty();
                    } else {
                        Member member = event.getMember();
                        String memberID = member.getId().asString();
                        String memberMention = DiscordUtilities.getMention(memberID);
                        Account account = sqlRunner.getAccountFromDiscordID(memberID);

                        boolean isVerified;
                        if (account == null) {
                            isVerified = false;
                        } else {
                            isVerified = sqlRunner.isVerified(account.getAccountID(), serverID.asString());
                        }

                        Mono<Void> sendMessageOnJoin;
                        Mono<Void> addDefaultRoleOnJoin;
                        
                        if (isVerified) {
                            boolean isBanned = sqlRunner.isBanned(account.getUserID(), serverID.asString());
                            if (!isBanned) {
                                addDefaultRoleOnJoin = member.addRole(verifiedRoleID, "Assign verified role on join").then();
                                sendMessageOnJoin = gateway.getChannelById(verificationChannelID)
                                        .ofType(GuildMessageChannel.class)
                                        .flatMap(channel -> channel.createMessage("Welcome to the server " + memberMention + "! I can see that you've already verified here before so I've assigned you the verified role!")).then();
                            } else {
                                Mono<Void> banMemberMono = member.ban().then();
                                sendMessageOnJoin = gateway.getChannelById(adminChannelID)
                                        .ofType(GuildMessageChannel.class)
                                        .flatMap(channel -> channel.createMessage(memberMention + ", who was verified on another account tried to join the server, as a result they have been banned.")).then();
                                return banMemberMono.and(sendMessageOnJoin);
                            }
                        } else {
                            addDefaultRoleOnJoin = member.addRole(unverifiedRoleID, "Assign default role on join").then();
                            if (mode.equals("SLASH")) {
                                sendMessageOnJoin = gateway.getChannelById(verificationChannelID)
                                        .ofType(GuildMessageChannel.class)
                                        .flatMap(channel -> channel.createMessage("Welcome to the server " + memberMention + " before you're able to fully interact with the server you need to verify your account. Start by entering your student number into the slash command \"/begin <student_number>\"!")).then();

                            } else {
                                Button button = Button.primary(BUTTON_ID + ":" + BUTTON_VERIFY + ":" + memberID, "Verify");

                                MessageCreateSpec message = MessageCreateSpec.builder()
                                        .content("Welcome to the server " + memberMention + " before you're able to fully interact with the server you need to verify your account. Start by pressing the verify button!")
                                        .addComponent(ActionRow.of(button))
                                        .build();

                                sendMessageOnJoin = gateway.getChannelById(verificationChannelID)
                                        .ofType(GuildMessageChannel.class)
                                        .flatMap(channel -> channel.createMessage(message)).then();

                            }
                        }
                        return sendMessageOnJoin.then(addDefaultRoleOnJoin);
                    }
                }).then();

                Mono<Void> actOnBan = gateway.on(BanEvent.class, event -> {
                    String memberID = event.getUser().getId().asString();
                    Snowflake botSnowflake = gateway.getSelfId();
                    String memberMention = DiscordUtilities.getMention(memberID);
                    String message = memberMention + WAS_BANNED;
                    Snowflake guildID = event.getGuildId();
                    GuildData guildData = guildDataMap.get(guildID);

                    Mono<Void> sendMessageMono = gateway.getChannelById(guildData.getAdminChannelID())
                            .ofType(GuildMessageChannel.class)
                            .flatMap(channel -> channel.createMessage(message))
                            .then();

                    Mono<Void> banAltsMono = event.getGuild()
                            .flatMap(guild -> guild.getAuditLog().withActionType(ActionType.MEMBER_BAN_ADD)
                                    .take(1)
                                    .flatMap(auditLogPart -> {
                                        //Check that most recent ban was not the bot, otherwise banning a user would trigger the bot to trigger itself again
                                        AuditLogEntry mostRecentBan = auditLogPart.getEntries().get(0);
                                        Snowflake responsibleUser = mostRecentBan.getResponsibleUserId().get();
                                        if (responsibleUser.equals(botSnowflake)) {
                                            return Mono.empty().then();
                                        } else {
                                            //Code for if the ban was not the bot
                                            Account account = sqlRunner.getAccountFromDiscordID(memberID);
                                            //Account will be null if the user never began verification, so we can't do anything
                                            if (!(account == null)) {
                                                String userID = account.getUserID();
                                                ArrayList<Account> accounts = sqlRunner.getAccountsFromUserID(userID);
                                                //Ban any alts
                                                Mono<Void> banUserMono = Mono.empty();
                                                for (Account currentAccount : accounts) {
                                                    //Ensure we're not trying to ban the account that was just banned
                                                    if (!currentAccount.getDiscordID().equals(memberID)) {
                                                        banUserMono = banUserMono.and(
                                                                guild.ban(Snowflake.of(currentAccount.getDiscordID())).onErrorResume(throwable -> {
                                                                            throwable.printStackTrace();
                                                                            return gateway.getChannelById(guildData.getAdminChannelID())
                                                                                    .ofType(GuildMessageChannel.class)
                                                                                    .flatMap(channel -> channel.createMessage("Failed to ban: " + DiscordUtilities.getMention(currentAccount.getDiscordID())))
                                                                                    .then();
                                                                        })
                                                                        .then());
                                                    }
                                                }


                                                //Insert ban into db
                                                sqlRunner.insertBan(userID, event.getGuildId().asString());
                                                return banUserMono;
                                            } else {
                                                return Mono.empty().then();
                                            }
                                        }
                                    }).then());
                    return banAltsMono.and(sendMessageMono);
                }).then();

                Mono<Void> actOnUnban = gateway.on(UnbanEvent.class, event -> {
                    String memberID = event.getUser().getId().asString();
                    Snowflake guildID = event.getGuildId();
                    GuildData guildData = guildDataMap.get(guildID);
                    String memberMention = DiscordUtilities.getMention(memberID);
                    String message = memberMention + WAS_UNBANNED;

                    Mono<Void> sendMessageMono = gateway.getChannelById(guildData.getAdminChannelID())
                            .ofType(GuildMessageChannel.class)
                            .flatMap(channel -> channel.createMessage(message))
                            .then();

                    Snowflake botSnowflake = gateway.getSelfId();
                    Mono<Void> unbanAltsMono = event.getGuild()
                            .flatMap(guild -> guild.getAuditLog().withActionType(ActionType.MEMBER_BAN_REMOVE)
                                    .take(1)
                                    .flatMap(auditLogPart -> {
                                        //Check that most recent unban was not the bot, otherwise banning a user would trigger the bot to trigger itself again
                                        AuditLogEntry mostRecentBan = auditLogPart.getEntries().get(0);
                                        Snowflake responsibleUser = mostRecentBan.getResponsibleUserId().get();
                                        if (responsibleUser.equals(botSnowflake)) {
                                            return Mono.empty().then();
                                        } else {
                                            Account account = sqlRunner.getAccountFromDiscordID(memberID);
                                            //Account will be null if the user never began verification, so we can't do anything
                                            if (!(account == null)) {
                                                String userID = account.getUserID();
                                                ArrayList<Account> accounts = sqlRunner.getAccountsFromUserID(userID);
                                                //Ban any alts
                                                Mono<Void> unbanUserMono = Mono.empty();
                                                for (Account currentAccount : accounts) {
                                                    //Ensure we're not trying to ban the account that was just banned
                                                    if (!currentAccount.getDiscordID().equals(memberID)) {
                                                        unbanUserMono = unbanUserMono.and(
                                                                guild.unban(Snowflake.of(currentAccount.getDiscordID())).onErrorResume(throwable -> {
                                                                            throwable.printStackTrace();
                                                                            return gateway.getChannelById(guildData.getAdminChannelID())
                                                                                    .ofType(GuildMessageChannel.class)
                                                                                    .flatMap(channel -> channel.createMessage("Failed to unban: " + DiscordUtilities.getMention(currentAccount.getDiscordID())))
                                                                                    .then();
                                                                        })
                                                                        .then());
                                                    }
                                                }
                                                //Insert ban into db
                                                sqlRunner.deleteBan(userID, guildID.asString());
                                                return unbanUserMono;
                                            } else {
                                                return Mono.empty().then();
                                            }
                                        }
                                    }).then());
                    return unbanAltsMono.and(sendMessageMono);
                }).then();

                Mono<Void> actOnSlashCommand = gateway.on(new ReactiveEventAdapter() {
                    @Override
                    public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
                        event.deferReply().withEphemeral(true).subscribe();
                        String commandName = event.getCommandName();
                        Snowflake guildSnowflake = event.getInteraction().getGuildId().get();
                        String result = null;
                        if (!guildDataMap.containsKey(guildSnowflake)) {
                            guildDataMap.put(guildSnowflake, GuildData.emptyGuildData(guildSnowflake.asString()));
                        }
                        boolean isServerConfigured = (guildDataMap.get(guildSnowflake).getVerifiedRoleID() != null);
                        String discordID = event.getInteraction().getMember().get().getId().asString();

                        if (commandName.equals(BEGIN_COMMAND_NAME)) {
                            String studentNumber = event.getOption(STUDENT_ID_OPTION).get().getValue().get().asString();
                            return event.getInteraction().getGuild()
                                    .map(Guild::getName)
                                    .flatMap(name -> {
                                        String resultToReturn = VerificationUtilities.beginVerification(guildDataMap.get(guildSnowflake), studentNumber, sqlRunner, event.getInteraction().getMember().get(), emailSender, name);
                                        if (resultToReturn.equals(USER_IS_BANNED_RESULT)) {
                                            Mono<Void> ban = event.getInteraction().getMember().get().ban().then();
                                            return event.editReply(resultToReturn).and(ban);
                                        }
                                        return event.editReply(resultToReturn);
                                    });
                        }
                        if (commandName.equals(VERIFY_COMMAND_NAME)) {
                            String tokenInput = event.getOption(VERIFICATION_CODE_OPTION).get().getValue().get().asString();
                            result = VerificationUtilities.finaliseVerification(tokenInput, isServerConfigured, sqlRunner, discordID, guildSnowflake);
                            if (result.equals(VERIFY_COMMAND_SUCCESS)) {
                                Mono<Void> removeUnverifiedRoleMono = event.getInteraction().getMember()
                                        .map(member -> member.removeRole(guildDataMap.get(guildSnowflake).getUnverifiedRoleID()))
                                        .get()
                                        .then();

                                Mono<Void> addVerifiedRoleMono = event.getInteraction().getMember()
                                        .map(member -> member.addRole(guildDataMap.get(guildSnowflake).getVerifiedRoleID()))
                                        .get()
                                        .then();
                                return event.editReply(result).and(removeUnverifiedRoleMono).and(addVerifiedRoleMono);
                            } else {
                                return event.editReply(result);
                            }
                        }
                        if (commandName.equals(SETUP_COMMAND_NAME)) {
                            //Get inputs
                            String verificationChannel = event.getOption(VERIFICATION_CHANNEL_OPTION).get().getValue().get().asSnowflake().asString();
                            String adminChannel = event.getOption(ADMIN_CHANNEL_OPTION).get().getValue().get().asSnowflake().asString();
                            String unverifiedRole = event.getOption(UNVERIFIED_ROLE_OPTION).get().getValue().get().asSnowflake().asString();
                            String verifiedRole = event.getOption(VERIFIED_ROLE_OPTION).get().getValue().get().asSnowflake().asString();
                            boolean applyUnverified = event.getOption(APPLY_UNVERIFIED_OPTION).get().getValue().get().asBoolean();
                            boolean useButtons = event.getOption(BUTTON_MODE_OPTION).get().getValue().get().asBoolean();

                            try {
                                Snowflake verificationChannelSnowflake = Snowflake.of(verificationChannel);
                                Snowflake adminChannelSnowflake = Snowflake.of(adminChannel);
                                Snowflake unverifiedRoleSnowflake = Snowflake.of(unverifiedRole);
                                Snowflake verifiedRoleSnowflake = Snowflake.of(verifiedRole);
                                return event.getInteraction()
                                        .getMember()
                                        .get()
                                        .getBasePermissions()
                                        .map(perms -> perms.contains(Permission.ADMINISTRATOR))
                                        .flatMap(hasAdmin -> {
                                            if (hasAdmin) {
                                                //Enter the new configuration into the database
                                                String guildID = guildSnowflake.asString();
                                                if (!sqlRunner.dbHasGuild(guildID)) {
                                                    sqlRunner.insertGuild(guildID);
                                                }
                                                if (!sqlRunner.updateGuildData(adminChannel, verificationChannel, unverifiedRole, verifiedRole, useButtons, guildSnowflake.asString())) {
                                                    return event.editReply(SETUP_COMMAND_ERROR);
                                                }

                                                //Edit the existing GuildData for this server
                                                GuildData guildData = guildDataMap.get(guildSnowflake);
                                                guildData.setAdminChannelID(adminChannelSnowflake);
                                                guildData.setVerificationChannelID(verificationChannelSnowflake);
                                                guildData.setUnverifiedRoleID(unverifiedRoleSnowflake);
                                                guildData.setVerifiedRoleID(verifiedRoleSnowflake);
                                                if (useButtons) {
                                                    guildData.setMode("MODAL");
                                                }

                                                Mono applyRolesMono = Mono.empty();
                                                if (applyUnverified) {
                                                    applyRolesMono = gateway.getGuildMembers(guildSnowflake)
                                                            .collectList()
                                                            .flatMap(members -> {
                                                                Mono<Void> addRoleToMembers = Mono.empty();
                                                                for (int i = 0; i < members.size(); i++) {
                                                                    Member member = members.get(i);
                                                                    boolean hasUnverifiedRole = member.getRoleIds().contains(unverifiedRoleSnowflake);
                                                                    boolean hasVerifiedRole = member.getRoleIds().contains(verifiedRoleSnowflake);

                                                                    if (!hasUnverifiedRole && !hasVerifiedRole) {
                                                                        Mono<Void> addRoleToMember = member.addRole(unverifiedRoleSnowflake);
                                                                        addRoleToMembers = addRoleToMembers.and(addRoleToMember);
                                                                    }
                                                                }
                                                                return addRoleToMembers;
                                                            })
                                                            .then();
                                                }

                                                return event.editReply(SETUP_COMMAND_SUCCESS).and(applyRolesMono);
                                            } else {
                                                return event.editReply(INSUFFICIENT_PERMISSIONS_ERROR);
                                            }
                                        });
                            } catch (NumberFormatException numberFormatException) {
                                return event.editReply("There is an error with one of your options, please try again...");
                            }
                        }
                        if (commandName.equals(HELP_COMMAND_NAME)) {
                            result = HELP_COMMAND_SUCCESS;
                        }
                        if (commandName.equals(NON_STUDENT_VERIFY_COMMAND_NAME)) {
                            String memberID = event.getInteraction().getMember().get().getId().asString();
                            if (isServerConfigured) {
                                boolean hasVerifiedRole = event.getInteraction().getMember().get().getRoleIds().contains(guildDataMap.get(guildSnowflake).getVerifiedRoleID());
                                if (!hasVerifiedRole) {
                                    if (manualVerificationsMap.get(memberID) == null || manualVerificationsMap.get(memberID) < MAX_VERIFICATION_REQUESTS) {
                                        String reason = event.getOption(REASON_OPTION).get().getValue().get().asString();


                                        Snowflake adminChannelID = guildDataMap.get(guildSnowflake).getAdminChannelID();
                                        Button acceptButton = Button.success(BUTTON_ID + ":" + BUTTON_ACCEPT + ":" + memberID, "Accept");
                                        Button denyButton = Button.danger(BUTTON_ID + ":" + BUTTON_DENY + ":" + memberID, "Deny");

                                        String memberMention = DiscordUtilities.getMention(memberID);

                                        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                                .title("A user has requested manual verification!")
                                                .color(EMBED_COLOUR)
                                                .description(memberMention + " gave the following reason: " + reason)
                                                .footer(EmbedCreateFields.Footer.of("SwanAuth | " + StringUtilities.getDateTime(), FOOTER_ICON_URL))
                                                .build();

                                        MessageCreateSpec message = MessageCreateSpec.builder()
                                                .addEmbed(embed)
                                                .addComponent(ActionRow.of(acceptButton, denyButton))
                                                .build();

                                        Mono<Message> sendMessageToAdmins = gateway.getChannelById(adminChannelID)
                                                .ofType(GuildMessageChannel.class)
                                                .flatMap(channel -> channel.createMessage(message));

                                        manualVerificationsMap.merge(memberID, 1, Integer::sum);
                                        return event.editReply(MANUAL_VERIFICATION_COMMAND_SUCCESS).and(sendMessageToAdmins);
                                    } else {
                                        return event.editReply(TOO_MANY_VERIFICATION_REQUESTS_ERROR);
                                    }
                                } else {
                                    return event.editReply(ACCOUNT_ALREADY_VERIFIED_ERROR);
                                }
                            } else {
                                result = SERVER_NOT_CONFIGURED_ERROR;
                            }
                        }
                        if (result == null) {
                            result = DEFAULT_ERROR;
                        }
                        return event.editReply(result);
                    }
                }).then();


                Mono<Void> buttonListener = gateway.on(ButtonInteractionEvent.class, event -> {
                    if (event.getCustomId().startsWith(BUTTON_ID)) {
                        String[] buttonInfo = event.getCustomId().split(":");
                        String buttonPressed = buttonInfo[1];
                        Snowflake memberSnowflake = Snowflake.of(buttonInfo[2]);
                        String memberID = memberSnowflake.asString();
                        return event.getInteraction()
                                .getMember()
                                .get()
                                .getBasePermissions()
                                .map(perms -> perms.contains(Permission.ADMINISTRATOR))
                                .flatMap(hasAdmin -> {
                                    if (buttonPressed.equals(BUTTON_VERIFY)) {
                                        //Code for verify button
                                        InteractionPresentModalSpec modal = InteractionPresentModalSpec.builder()
                                                .title("SwanAuth Verification")
                                                .customId(MODAL_ID)
                                                .addComponent(ActionRow.of(TextInput.small(
                                                                INPUT_ID + ":" + INPUT_STUDENT + ":" + memberID, "Student Number")
                                                        .required(true)))
                                                .build();

                                        return event.presentModal(modal);
                                    } else if (buttonPressed.equals(BUTTON_FINISH_VERIFICATION)) {
                                        InteractionPresentModalSpec modal = InteractionPresentModalSpec.builder()
                                                .title("SwanAuth Verification")
                                                .customId(MODAL_FINISH_ID)
                                                .addComponent(ActionRow.of(TextInput.small(
                                                                INPUT_ID + ":" + INPUT_CODE + ":" + memberID, "Verification Code")
                                                        .required(true)))
                                                .build();

                                        return event.presentModal(modal);
                                    } else if (hasAdmin) {
                                        String memberMention = DiscordUtilities.getMention(memberID);
                                        Snowflake guildSnowflake = event.getInteraction().getGuildId().get();
                                        Snowflake verifiedRole = guildDataMap.get(guildSnowflake).getVerifiedRoleID();
                                        Snowflake unverifiedRole = guildDataMap.get(guildSnowflake).getUnverifiedRoleID();
                                        Snowflake verificationChannel = guildDataMap.get(guildSnowflake).getVerificationChannelID();


                                        List<LayoutComponent> layoutComponents = List.of();
                                        if (buttonPressed.equals(BUTTON_ACCEPT)) {
                                            Embed oldEmbed = event.getMessage().get().getEmbeds().get(0);
                                            Mono<Void> notifyMemberOfResult = gateway.getChannelById(verificationChannel)
                                                    .ofType(GuildMessageChannel.class)
                                                    .flatMap(channel -> channel.createMessage(memberMention + " - Your manual verification has been completed!"))
                                                    .then();

                                            Mono<Void> giveMemberVerifiedRole = event.getInteraction().getGuild()
                                                    .flatMap(guild -> guild.getMemberById(memberSnowflake))
                                                    .flatMap(member -> member.addRole(verifiedRole).then(member.removeRole(unverifiedRole)));


                                            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                                    .title("A user has been accepted for manual verification!")
                                                    .color(EMBED_COLOUR)
                                                    .description(oldEmbed.getDescription().get())
                                                    .footer(EmbedCreateFields.Footer.of("SwanAuth | " + StringUtilities.getDateTime(), FOOTER_ICON_URL))
                                                    .build();

                                            MessageEditSpec editSpec = MessageEditSpec.builder()
                                                    .components(layoutComponents)
                                                    .embeds(List.of(embed))
                                                    .build();

                                            Mono<Message> removeButtons = event.getMessage().get().edit(editSpec);

                                            manualVerificationsMap.remove(memberSnowflake.asString());
                                            return event.reply("The user has been verified successfully!").withEphemeral(true).then(giveMemberVerifiedRole).then(notifyMemberOfResult).and(removeButtons);
                                        } else if (buttonPressed.equals(BUTTON_DENY)) {
                                            Embed oldEmbed = event.getMessage().get().getEmbeds().get(0);
                                            Mono<Void> notifyMemberOfResult = gateway.getChannelById(verificationChannel)
                                                    .ofType(GuildMessageChannel.class)
                                                    .flatMap(channel -> channel.createMessage(memberMention + " - Your manual verification has been denied, if you think this in an error please contact a server admin."))
                                                    .then();

                                            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                                    .title("A user has been denied manual verification!")
                                                    .color(EMBED_COLOUR)
                                                    .description(oldEmbed.getDescription().get())
                                                    .footer(EmbedCreateFields.Footer.of("SwanAuth | " + StringUtilities.getDateTime(), FOOTER_ICON_URL))
                                                    .build();

                                            MessageEditSpec editSpec = MessageEditSpec.builder()
                                                    .components(layoutComponents)
                                                    .embeds(List.of(embed))
                                                    .build();

                                            Mono<Message> removeButtons = event.getMessage().get().edit(editSpec);

                                            manualVerificationsMap.remove(memberSnowflake.asString());
                                            return event.reply("The user has been denied manual verification and notified accordingly").withEphemeral(true).and(notifyMemberOfResult).and(removeButtons);
                                        } else {
                                            return event.reply(DEFAULT_ERROR).withEphemeral(true);
                                        }
                                    } else {
                                        return event.reply(INSUFFICIENT_PERMISSIONS_ERROR).withEphemeral(true);
                                    }
                                });
                    } else {
                        // Ignore it
                        return Mono.empty();
                    }
                }).then();

                Mono<Void> modalListener = gateway.on(ModalSubmitInteractionEvent.class, event -> {
                    String modalID = event.getCustomId();
                    if (modalID.equals(MODAL_ID)) {
                        for (TextInput textInput : event.getComponents(TextInput.class)) {
                            String inputID = textInput.getCustomId();
                            if (inputID.startsWith(INPUT_ID)) {
                                String studentNumber = textInput.getValue().get();
                                Snowflake guildSnowflake = event.getInteraction().getGuildId().get();
                                return event.getInteraction().getGuild()
                                        .map(Guild::getName)
                                        .flatMap(name -> {
                                            String resultToReturn = VerificationUtilities.beginVerification(guildDataMap.get(guildSnowflake), studentNumber, sqlRunner, event.getInteraction().getMember().get(), emailSender, name);
                                            if (resultToReturn.equals(USER_IS_BANNED_RESULT)) {
                                                Mono<Void> ban = event.getInteraction().getMember().get().ban().then();
                                                return event.reply(resultToReturn).withEphemeral(true).and(ban);
                                            }
                                            if (resultToReturn.equals(BEGIN_COMMAND_SUCCESS_RESULT)) {
                                                resultToReturn = BEGIN_COMMAND_SUCCESS_RESULT_MODAL;
                                                Button button = Button.primary(BUTTON_ID + ":" + BUTTON_FINISH_VERIFICATION + ":" + event.getInteraction().getMember().get().getId().asString(), "Enter Verification Code");
                                                return event.reply(resultToReturn).withComponents(ActionRow.of(button)).withEphemeral(true);
                                            } else {
                                                return event.reply(resultToReturn).withEphemeral(true);
                                            }
                                        });
                            }
                        }
                    } else if (modalID.equals(MODAL_FINISH_ID)) {
                        for (TextInput textInput : event.getComponents(TextInput.class)) {
                            String inputID = textInput.getCustomId();
                            if (inputID.startsWith(INPUT_ID)) {
                                String verificationCode = textInput.getValue().get();
                                String memberID = event.getInteraction().getMember().get().getId().asString();
                                Snowflake guildID = event.getInteraction().getGuildId().get();
                                GuildData guildData = guildDataMap.get(guildID);
                                boolean isServerConfigured = (guildData.getVerifiedRoleID() != null);
                                String result = VerificationUtilities.finaliseVerification(verificationCode, isServerConfigured, sqlRunner, memberID, guildID);
                                if (result.equals(VERIFY_COMMAND_SUCCESS)) {
                                    Mono<Void> removeUnverifiedRoleMono = event.getInteraction().getMember()
                                            .map(member -> member.removeRole(guildDataMap.get(guildID).getUnverifiedRoleID()))
                                            .get();

                                    Mono<Void> addVerifiedRoleMono = event.getInteraction().getMember()
                                            .map(member -> member.addRole(guildDataMap.get(guildID).getVerifiedRoleID()))
                                            .get();

                                    return event.reply(result).withEphemeral(true).and(removeUnverifiedRoleMono).and(addVerifiedRoleMono);
                                } else {
                                    return event.reply(result).withEphemeral(true);
                                }
                            }
                        }
                    }
                    return Mono.empty();
                }).then();

                // combine them!
                return doOnEachGuild.and(actOnJoin).and(actOnBan).and(actOnUnban).and(actOnSlashCommand).and(buttonListener).and(createGlobalApplicationCommands).and(modalListener);
            });

            login.block();
        } else {
            System.out.println(INCORRECT_COMMANDLINE_ARGUMENTS_ERROR);
            System.exit(1);
        }
    }
}
