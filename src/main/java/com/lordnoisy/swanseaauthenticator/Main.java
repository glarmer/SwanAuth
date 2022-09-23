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
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static final Logger LOG = Loggers.getLogger(GuildCommandRegistrar.class);

    public static final String ACCOUNT_ALREADY_VERIFIED_ERROR = "This discord account is already verified on this server!";
    public static final String BEGIN_COMMAND_SUCCESS_RESULT = "Please check your student email for a verification code, make sure to check your spam as it might've been sent there. Once you have your code, use /verify to finish verifying.";
    public static final String DATABASE_ERROR = "There was an error contacting the database, please try again or contact the bot admin for help";
    public static final String HELP_COMMAND_SUCCESS = "To begin verification run /verify! Admins can run /setup to configure the bot!";
    public static final String INCORRECT_COMMANDLINE_ARGUMENTS_ERROR = "You have entered the incorrect amount of command line arguments, please check that your mySQL and Email login is entered correctly.";
    public static final String INCORRECT_STUDENT_NUMBER_ERROR = "The student number you entered was incorrect, please try again! If you do not have a student number (e.g. if you are a staff member or alumni) please contact a moderator of this server!";
    public static final String INCORRECT_TOKEN_ERROR = "The verification token you entered is incorrect, please try again...";
    public static final String INSUFFICIENT_PERMISSIONS_ERROR = "You don't have permissions to perform this command! You need to be an administrator on the server to do this.";
    public static final String INVALID_ADMIN_CHANNEL_ERROR = "The admin channel ID you entered does not seem to exist in this server. ";
    public static final String INVALID_UNVERIFIED_ROLE_ERROR = "The unverified role ID you entered does not seem to exist in this server. ";
    public static final String INVALID_VERIFICATION_CHANNEL_ERROR = "The verification channel ID you entered does not seem to exist in this server. ";
    public static final String INVALID_VERIFIED_ROLE_ERROR = "The verified role ID you entered does not seem to exist in this server. ";
    public static final String SETUP_COMMAND_FAILURE = "Configuring bot failed, please try again or contact the bot admin.";
    public static final String SETUP_COMMAND_SUCCESS = "You have successfully configured the bot!";
    public static final String TOO_MANY_ATTEMPTS_ERROR = "You have made too many attempts to begin verification recently, please either verify using an existing token or try again later.";
    public static final String USER_IS_BANNED_RESULT = "This user has been banned on a different Discord account, and so is no longer allowed on this server...";
    public static final String VERIFY_COMMAND_SUCCESS = "You have successfully verified!";
    public static final String SERVER_NOT_CONFIGURED_ERROR = "The server admins haven't configured the bot yet, contact them for assistance.";
    public static final String FOOTER_ICON_URL = "https://media.discordapp.net/attachments/1020458334882631690/1022266062579974184/SwanAuth.png?width=910&height=910";
    public static final String MANUAL_VERIFICATION_COMMAND_SUCCESS = "Your manual verification request has been sent to the admins!";
    public static final String NON_STUDENT_VERIFY_COMMAND_NAME = "nonstudentverify";
    public static final String SETUP_COMMAND_NAME = "setup";
    public static final String HELP_COMMAND_NAME = "help";
    public static final String VERIFY_COMMAND_NAME = "verify";
    public static final String BEGIN_COMMAND_NAME = "begin";

    //0 Token 1 MYSQL URL 2 MYSQL Username 3 MYSQL password 4 Email Host 5 Email port 6 Email username 7 Email password 8 Sender Email Address
    public static void main(String[] args) {
        if (args.length == 9) {
            String token = args[0];
            DataSource databaseConnector = new DataSource(args[1], args[2], args[3]);
            SQLRunner sqlRunner = new SQLRunner(databaseConnector);
            EmailSender emailSender = new EmailSender(args[4], Integer.parseInt(args[5]), args[6], args[7], args[8]);
            sqlRunner.databaseConfiguredCheck();

            // Creates a map containing details of each server stored in the mySQL db
            final Map<Snowflake, GuildData> guildDataMap = new HashMap<>();
            sqlRunner.populateGuildMapFromDatabase(guildDataMap);

            DiscordClient client = DiscordClient.create(token);
            Mono<Void> login = DiscordClient.create(token).gateway().setEnabledIntents(IntentSet.all()).withGateway((GatewayDiscordClient gateway) -> {
                //TODO: Look into the exact intents I need, since I am no longer reading messages
                client.gateway().setEnabledIntents(IntentSet.all());

                // Make commands
                ApplicationCommandRequest beginCommand = ApplicationCommandRequest.builder()
                        .name(BEGIN_COMMAND_NAME)
                        .description("Begin the verification process by entering your Student ID")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("student_id")
                                .description("Your Student ID")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest verifyCommand = ApplicationCommandRequest.builder()
                        .name(VERIFY_COMMAND_NAME)
                        .description("Finish verifying by entering your verification token :)")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("verification_code")
                                .description("The verification code you received via email!")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest nonStudentVerifyCommand = ApplicationCommandRequest.builder()
                        .name(NON_STUDENT_VERIFY_COMMAND_NAME)
                        .description("Use this to ask for verification if you do not have a student number")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("reason")
                                .description("The reason why you need to manually verify, e.g. you are a staff member...")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .maxLength(4000)
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest setupCommand = ApplicationCommandRequest.builder()
                        .name(SETUP_COMMAND_NAME)
                        .description("Configure the bot so it can begin verifying users")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("verification_channel")
                                .description("The channel that you want the bot to use for verifications.")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("admin_channel")
                                .description("The channel that you want the bot to use for admin messages.")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("unverified_role")
                                .description("The unverified role you want to apply to users when they join the server.")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("verified_role")
                                .description("The verified role you want to apply to users after they verify.")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest helpCommand = ApplicationCommandRequest.builder()
                        .name(HELP_COMMAND_NAME)
                        .description("Run this command to get help on how to use the bot!")
                        .build();

                List<ApplicationCommandRequest> applicationCommandRequestList = List.of(beginCommand, verifyCommand, helpCommand, setupCommand, nonStudentVerifyCommand);


                Mono<Void> doOnEachGuild = gateway.getGuilds().doOnEach(guild -> {
                    try {
                        Snowflake guildID = guild.get().getId();
                        // Check for guilds that are present but not in the map, and thus not in the db. This could happen if they invite the bot when it's offline
                        if (guildDataMap.get(guildID) == null) {
                            sqlRunner.insertGuild(guildID.asString());
                            GuildData guildData = new GuildData(guildID.asString(), null, null, null, null);
                            guildDataMap.put(guildID, guildData);
                        }

                        // Register commands in each guild
                        GuildCommandRegistrar.create(gateway.getRestClient(), guildID.asLong(), applicationCommandRequestList)
                                .registerCommands()
                                .doOnError(e -> LOG.warn("Unable to create guild command", e))
                                .onErrorResume(e -> Mono.empty())
                                .blockLast();

                    } catch (NullPointerException npe) {
                        System.out.println("Continuing");
                    }
                }).then();

                Mono<Void> actOnJoin = gateway.on(MemberJoinEvent.class, event -> {
                    Snowflake serverID = event.getGuildId();
                    GuildData guildData = guildDataMap.get(serverID);
                    Snowflake unverifiedRoleID = guildData.getUnverifiedRoleID();
                    Snowflake verifiedRoleID = guildData.getVerifiedRoleID();
                    Snowflake verificationChannelID = guildData.getVerificationChannelID();
                    Snowflake adminChannelID = guildData.getAdminChannelID();
                    if (unverifiedRoleID == null || verificationChannelID == null) {
                        return Mono.empty();
                    } else {
                        Member member = event.getMember();
                        String memberMention = "<@" + member.getId().asString() + ">";
                        Account account = sqlRunner.getAccountFromDiscordID(member.getId().asString());

                        boolean isVerified = sqlRunner.isVerified(account.getAccountID(), serverID.asString());
                        Mono<Void> sendMessageOnJoin;

                        Mono<Void> addDefaultRoleOnJoin;
                        if (isVerified) {
                            boolean isBanned = sqlRunner.isBanned(account.getUserID(), serverID.asString());
                            if (!isBanned){
                                addDefaultRoleOnJoin = member.addRole(verifiedRoleID, "Assign verified role on join").then();
                                sendMessageOnJoin = gateway.getChannelById(verificationChannelID)
                                        .flatMap(channel -> channel.getRestChannel().createMessage("Welcome to the server " + memberMention + "! I can see that you've already verified here before so I've assigned you the verified role!")).then();
                            } else {
                                Mono<Void> banMemberMono = member.ban().then();
                                sendMessageOnJoin = gateway.getChannelById(adminChannelID)
                                        .flatMap(channel -> channel.getRestChannel().createMessage(memberMention + ", who was verified on another account tried to join the server, as a result they have been banned.")).then();
                                return banMemberMono.and(sendMessageOnJoin);
                            }
                        } else {
                            addDefaultRoleOnJoin = member.addRole(unverifiedRoleID, "Assign default role on join").then();
                            sendMessageOnJoin = gateway.getChannelById(verificationChannelID)
                                    .flatMap(channel -> channel.getRestChannel().createMessage("Welcome to the server " + memberMention + " before you're able to fully interact with the server you need to verify your account. Start by entering your student number into the slash command \"/begin <student_number>\"!")).then();
                        }
                        return addDefaultRoleOnJoin.and(sendMessageOnJoin);
                    }
                }).then();

                Mono<Void> actOnBan = gateway.on(BanEvent.class, event -> {
                    String memberID = event.getUser().getId().asString();
                    Account account = sqlRunner.getAccountFromDiscordID(memberID);
                    //Account will be null if the user never began verification, so we can't do anything
                    if (!(account == null)) {
                        String userID = account.getUserID();
                        ArrayList<Account> accounts = sqlRunner.getAccountsFromUserID(userID);
                        Snowflake guildID = event.getGuildId();
                        GuildData guildData = guildDataMap.get(guildID);
                        //Ban any alts
                        Mono<Void> banUserMono = Mono.empty();
                        for (int i = 0; i < accounts.size(); i++) {
                            Account currentAccount = accounts.get(i);
                            //Ensure we're not trying to ban the account that was just banned
                            if (!currentAccount.getDiscordID().equals(memberID)) {
                                banUserMono = banUserMono.and(event.getGuild()
                                        .flatMap(guild -> guild.ban(Snowflake.of(currentAccount.getDiscordID())))
                                        .then());
                            }
                        }

                        //Insert bans into db
                        sqlRunner.insertBan(userID, event.getGuildId().asString());

                        //TODO: Construct suitable message
                        Mono<Void> sendMessageMono = gateway.getChannelById(guildData.getAdminChannelID())
                                .flatMap(channel -> channel.getRestChannel().createMessage("Test"))
                                .then();

                        return banUserMono.and(sendMessageMono);
                    } else {
                        return Mono.empty();
                    }
                }).then();

                Mono<Void> actOnUnban = gateway.on(UnbanEvent.class, event -> {
                            String memberID = event.getUser().getId().asString();
                            Account account = sqlRunner.getAccountFromDiscordID(memberID);

                            //Account will be null if the user never began verification, so we can't do anything
                            if (!(account == null)) {
                                String userID = account.getUserID();
                                ArrayList<Account> accounts = sqlRunner.getAccountsFromUserID(userID);
                                Snowflake guildID = event.getGuildId();
                                GuildData guildData = guildDataMap.get(guildID);

                                //Unban any alts
                                Mono<Void> unbanUserMono = Mono.empty();
                                for (int i = 0; i < accounts.size(); i++) {
                                    Account currentAccount = accounts.get(i);
                                    //Ensure we're not trying to unban the account that was just unbanned
                                    if (!currentAccount.getDiscordID().equals(memberID)) {
                                        unbanUserMono = unbanUserMono.and(event.getGuild()
                                                .flatMap(guild -> guild.unban(Snowflake.of(currentAccount.getDiscordID())))
                                                .then());
                                    }
                                    //Delete bans from db
                                    sqlRunner.deleteBan(currentAccount.getUserID(), event.getGuildId().asString());
                                }

                                //TODO: Construct suitable message
                                Mono<Void> sendMessageMono = gateway.getChannelById(guildData.getAdminChannelID())
                                        .flatMap(channel -> channel.getRestChannel().createMessage("Test"))
                                        .then();

                                return unbanUserMono.and(sendMessageMono);
                            } else {
                                return Mono.empty();
                            }
                        }).then();


                //Logic for commands
                //TODO: Admin commands? /manualVerify (even though they could just add the role, it makes it more obvious)
                //TODO: Have a help command for users - maybe detect admin and give more advice?
                //TODO: unban command for ease
                Mono<Void> actOnSlashCommand = gateway.on(new ReactiveEventAdapter() {
                    @Override
                    public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
                        event.deferReply().subscribe();
                        Snowflake guildSnowflake = event.getInteraction().getGuildId().get();
                        String result = null;
                        boolean isServerConfigured = (guildDataMap.get(guildSnowflake).getVerifiedRoleID() != null);
                        String discordID = event.getInteraction().getMember().get().getId().asString();
                        AtomicReference<String> guildName = new AtomicReference<>();

                        event.getInteraction().getGuild()
                                .map(Guild::getName)
                                .subscribe(name -> guildName.set(name));

                        if (event.getCommandName().equals(BEGIN_COMMAND_NAME)) {
                            if (isServerConfigured) {
                                result = BEGIN_COMMAND_SUCCESS_RESULT;
                                String studentNumber = event.getOption("student_id").get().getValue().get().asString();
                                if (studentNumber.matches("\\d+")) {
                                    String userID = sqlRunner.getOrCreateUserIDFromStudentID(studentNumber);
                                    if (userID != null) {
                                        if (!sqlRunner.isBanned(userID, guildSnowflake.asString())) {
                                            String accountID = sqlRunner.getOrCreateAccountIDFromDiscordIDAndUserID(userID, discordID);
                                            if (accountID != null) {
                                                if (!sqlRunner.isVerified(accountID, guildSnowflake.asString())) {
                                                    //Check that there aren't 3 or more verification tokens made within the past 12 hours - discourages spam
                                                    int rows = sqlRunner.selectRecentVerificationTokens(accountID, guildSnowflake.asString());
                                                    if (rows == -1) {
                                                        result = DATABASE_ERROR;
                                                    } else if (rows < 3) {
                                                        String verificationCode = StringUtilities.getAlphaNumericString(20);
                                                        sqlRunner.insertVerificationToken(accountID, guildSnowflake.asString(), verificationCode);
                                                        emailSender.sendVerificationEmail(studentNumber, verificationCode, guildName.get());
                                                    } else {
                                                        result = TOO_MANY_ATTEMPTS_ERROR;
                                                    }
                                                } else {
                                                    result = ACCOUNT_ALREADY_VERIFIED_ERROR;
                                                }
                                            } else {
                                                result = DATABASE_ERROR;
                                            }
                                        } else {
                                            result = USER_IS_BANNED_RESULT;
                                            Mono<Void> ban = event.getInteraction().getMember().get().ban().then();
                                            return event.editReply(result).and(ban);
                                        }
                                    } else {
                                        result = DATABASE_ERROR;
                                    }
                                } else {
                                    result = INCORRECT_STUDENT_NUMBER_ERROR;
                                }
                            } else {
                                result = SERVER_NOT_CONFIGURED_ERROR;
                            }
                        }
                        if (event.getCommandName().equals(VERIFY_COMMAND_NAME)) {
                            String tokenInput = event.getOption("verification_code").get().getValue().get().asString();
                            if (isServerConfigured) {
                                result = VERIFY_COMMAND_SUCCESS;
                                String accountID = sqlRunner.getAccountFromDiscordID(discordID).getAccountID();
                                if (accountID != null) {
                                    if (!sqlRunner.isVerified(accountID, guildSnowflake.asString())) {
                                        int rows = sqlRunner.selectVerificationToken(accountID, guildSnowflake.asString(), tokenInput);
                                        if (rows > 0) {
                                            String guildID = event.getInteraction().getGuildId().get().asString();
                                            sqlRunner.insertVerification(accountID, guildID);
                                            sqlRunner.deleteVerificationTokens(accountID, guildID);
                                        } else if (rows == -1) {
                                            result = DATABASE_ERROR;
                                        } else {
                                            result = INCORRECT_TOKEN_ERROR;
                                        }
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
                                        result = ACCOUNT_ALREADY_VERIFIED_ERROR;
                                    }
                                } else {
                                    result = DATABASE_ERROR;
                                }
                            } else {
                                result = SERVER_NOT_CONFIGURED_ERROR;
                            }
                            return event.editReply(result);
                        }
                        if (event.getCommandName().equals(SETUP_COMMAND_NAME)) {
                            result = SETUP_COMMAND_SUCCESS;

                            //Get inputs
                            String verificationChannel = event.getOption("verification_channel").get().getValue().get().asString();
                            String adminChannel = event.getOption("admin_channel").get().getValue().get().asString();
                            String unverifiedRole = event.getOption("unverified_role").get().getValue().get().asString();
                            String verifiedRole = event.getOption("verified_role").get().getValue().get().asString();

                            //Validate the inputs
                            AtomicBoolean verificationChannelValid = new AtomicBoolean(false);
                            AtomicBoolean adminChannelValid = new AtomicBoolean(false);
                            AtomicBoolean unverifiedRoleValid = new AtomicBoolean(false);
                            AtomicBoolean verifiedRoleValid = new AtomicBoolean(false);

                            //Check if admin
                            AtomicBoolean admin = new AtomicBoolean(false);
                            event.getInteraction()
                                    .getMember()
                                    .get()
                                    .getBasePermissions()
                                    .map(perms -> perms.contains(Permission.ADMINISTRATOR))
                                    .subscribe(hasAdmin -> admin.set(hasAdmin));

                            if (!admin.get()){
                                result = INSUFFICIENT_PERMISSIONS_ERROR;
                                return event.editReply(result);
                            }


                            if (verificationChannel.matches("\\d+")) {

                                gateway.getChannelById(Snowflake.of(verificationChannel))
                                        .map(channel -> channel.getId().asString().equals(verificationChannel))
                                        .subscribe(hasChannel -> verificationChannelValid.set(hasChannel));
                            }
                            if (adminChannel.matches("\\d+")) {
                                gateway.getChannelById(Snowflake.of(adminChannel))
                                        .map(channel -> channel.getId().asString().equals(adminChannel))
                                        .subscribe(hasChannel -> adminChannelValid.set(hasChannel));
                            }
                            if (unverifiedRole.matches("\\d+")) {
                                gateway.getRoleById(guildSnowflake, Snowflake.of(unverifiedRole))
                                        .map(role -> role.getId().asString().equals(unverifiedRole))
                                        .subscribe(hasRole -> unverifiedRoleValid.set(hasRole));
                            }
                            if (verifiedRole.matches("\\d+")) {
                                gateway.getRoleById(guildSnowflake, Snowflake.of(verifiedRole))
                                        .map(role -> role.getId().asString().equals(verifiedRole))
                                        .subscribe(hasRole -> verifiedRoleValid.set(hasRole));
                            }

                            //Output any errors
                            if (!verificationChannelValid.get() || !adminChannelValid.get() || !unverifiedRoleValid.get() || !verifiedRoleValid.get()) {
                                result = "";
                                if (!verificationChannelValid.get()) {
                                    result = INVALID_VERIFICATION_CHANNEL_ERROR;
                                }
                                if (!adminChannelValid.get()) {
                                    result = result + INVALID_ADMIN_CHANNEL_ERROR;
                                }
                                if (!unverifiedRoleValid.get()) {
                                    result = result + INVALID_UNVERIFIED_ROLE_ERROR;
                                }
                                if (!verifiedRoleValid.get()) {
                                    result = result + INVALID_VERIFIED_ROLE_ERROR;
                                }
                                return event.editReply(result);
                            }

                            //Enter the new configuration into the database
                            if (!sqlRunner.updateGuildData(adminChannel, verificationChannel, unverifiedRole, verifiedRole, guildSnowflake.asString())) {
                                result = SETUP_COMMAND_FAILURE;
                                return event.editReply(result);
                            }

                            //Edit the existing GuildData for this server
                            GuildData guildData = guildDataMap.get(guildSnowflake);
                            guildData.setAdminChannelID(Snowflake.of(adminChannel));
                            guildData.setVerificationChannelID(Snowflake.of(verificationChannel));
                            guildData.setUnverifiedRoleID(Snowflake.of(unverifiedRole));
                            guildData.setVerifiedRoleID(Snowflake.of(verifiedRole));
                        }
                        if (event.getCommandName().equals(HELP_COMMAND_NAME)) {
                            result = HELP_COMMAND_SUCCESS;
                        }
                        if (event.getCommandName().equals(NON_STUDENT_VERIFY_COMMAND_NAME)) {
                            if (isServerConfigured) {
                                String reason = event.getOption("reason").get().getValue().get().asString();
                                String memberID = event.getInteraction().getMember().get().getId().asString();

                                Snowflake adminChannelID = guildDataMap.get(guildSnowflake).getAdminChannelID();
                                Button acceptButton = Button.success("swanauth:accept:"+memberID, "Accept");
                                Button denyButton = Button.danger("swanauth:deny:"+memberID, "Deny");

                                String memberMention = "<@" + memberID + ">";

                                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                        .title("A user has requested manual verification!")
                                        .color(Color.BLUE)
                                        .description(memberMention + " gave the following reason: " + reason)
                                        .footer(EmbedCreateFields.Footer.of("Swanauth | " + LocalDateTime.now(), FOOTER_ICON_URL))
                                        .build();

                                MessageCreateSpec message = MessageCreateSpec.builder()
                                        .addEmbed(embed)
                                        .addComponent(ActionRow.of(acceptButton, denyButton))
                                        .build();

                                Mono<Message> sendMessageToAdmins = gateway.getChannelById(adminChannelID)
                                        .ofType(GuildMessageChannel.class)
                                        .flatMap(channel -> channel.createMessage(message));

                                return event.editReply(MANUAL_VERIFICATION_COMMAND_SUCCESS).and(sendMessageToAdmins);
                            } else {
                                result = SERVER_NOT_CONFIGURED_ERROR;
                            }
                        }
                        return event.editReply(result);
                    }
                }).then();

                Mono<Void> buttonListener = gateway.on(ButtonInteractionEvent.class, event -> {
                            if (event.getCustomId().startsWith("swanauth")) {
                                return event.reply("You clicked me!").withEphemeral(true);
                            } else {
                                // Ignore it
                                return Mono.empty();
                            }
                        })
                        .then(); //Transform the flux to a mono

                // combine them!
                return doOnEachGuild.and(actOnJoin).and(actOnBan).and(actOnUnban).and(actOnSlashCommand).and(buttonListener);
            });

            login.block();
        } else {
          System.out.println(INCORRECT_COMMANDLINE_ARGUMENTS_ERROR);
          System.exit(1);
        }
    }
}
