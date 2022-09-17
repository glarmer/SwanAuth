package com.lordnoisy.swanseaauthenticator;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.Permission;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final Logger log = Loggers.getLogger(GuildCommandRegistrar.class);

    //0 Token 1 MYSQL URL 2 MYSQL Username 3 MYSQL password 4 Email Host 5 Email port 6 Email username 7 Email password 8 Sender Email Address
    public static void main(String[] args) {
        if (args.length == 9) {
            String token = args[0];
            DatabaseConnector databaseConnector = new DatabaseConnector(args[1], args[2], args[3]);
            Connection connection = databaseConnector.getDatabaseConnection();
            SQLRunner sqlRunner = new SQLRunner(connection);
            EmailSender emailSender = new EmailSender(args[4], Integer.valueOf(args[5]), args[6], args[7], args[8]);

            //Check if mysql database is set up, set it up if it isn't.
            try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tableCheck = metaData.getTables(null, null, "guilds", null);
                if (tableCheck.next()) {
                    System.out.println("Database seems to exist... continuing.");
                }
                else {
                    sqlRunner.firstTimeSetup();
                    tableCheck.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }

            // Creates a map containing details of each server stored in the mySQL db
            final Map<Snowflake, GuildData> guildDataMap = new HashMap<>();
            try {
                ResultSet results = sqlRunner.getGuilds();
                while (results.next()) {
                    String currentGuildID = results.getString(1);
                    String currentAdminChannelID = results.getString(2);
                    String currentVerificationChannelID = results.getString(3);
                    String currentUnverifiedRoleID = results.getString(4);
                    String currentVerifiedRoleID = results.getString(5);
                    GuildData guildData = new GuildData(currentGuildID, currentAdminChannelID, currentVerificationChannelID, currentUnverifiedRoleID, currentVerifiedRoleID);
                    guildDataMap.put(Snowflake.of(currentGuildID), guildData);
                }
                results.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            DiscordClient client = DiscordClient.create(token);
            Mono<Void> login = DiscordClient.create(token).gateway().setEnabledIntents(IntentSet.all()).withGateway((GatewayDiscordClient gateway) -> {
                client.gateway().setEnabledIntents(IntentSet.all());

                // Make commands
                ApplicationCommandRequest beginCommand = ApplicationCommandRequest.builder()
                        .name("begin")
                        .description("Begin the verification process by entering your Student ID")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("student_id")
                                .description("Your Student ID")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest verifyCommand = ApplicationCommandRequest.builder()
                        .name("verify")
                        .description("Finish verifying by entering your verification token :)")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("verification_code")
                                .description("The verification code you received via email!")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest setupCommand = ApplicationCommandRequest.builder()
                        .name("setup")
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
                        .name("help")
                        .description("Run this command to get help on how to use the bot!")
                        .build();

                List<ApplicationCommandRequest> applicationCommandRequestList = List.of(beginCommand, verifyCommand, helpCommand, setupCommand);

                // Check for guilds that are present but not in the map, and thus not in the db. This could happen if they invite the bot when it's offline
                gateway.getGuilds().doOnEach(guild -> {
                    try {
                        Snowflake guildID = guild.get().getId();
                        if (guildDataMap.get(guildID) == null) {
                            System.out.println("Guild ID: "+ guildID.asString());
                            sqlRunner.insertGuild(guildID.asString());
                            GuildData guildData = new GuildData(guildID.asString(), null, null, null, null);
                            guildDataMap.put(guildID, guildData);
                        }

                        // Register commands in each guild
                        GuildCommandRegistrar.create(gateway.getRestClient(), guildID.asLong(), applicationCommandRequestList)
                                .registerCommands()
                                .doOnError(e -> log.warn("Unable to create guild command", e))
                                .onErrorResume(e -> Mono.empty())
                                .blockLast();

                    } catch (NullPointerException npe) {
                        System.out.println("Continuing");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        System.out.println("Couldn't insert guild... continuing but there might be issues.");
                    }
                }).then().subscribe();

                //TODO : Some logic for unbanning

                //TODO : Error handling for is default role is not set, essentially make it do nothing.
                Mono<Void> actOnJoin = gateway.on(MemberJoinEvent.class, event ->
                                Mono.fromRunnable(() -> {
                                    Snowflake serverID = event.getGuildId();
                                    GuildData guildData = guildDataMap.get(serverID);
                                    Snowflake unverifiedRoleID = guildData.getUnverifiedRoleID();
                                    Snowflake verificationChannelID = guildData.getVerificationChannelID();
                                    Member member = event.getMember();
                                    String memberMention = "<@" + member.getId().asString() + ">";

                                    member.addRole(unverifiedRoleID, "Assign default role on join");

                                    gateway.getChannelById(verificationChannelID)
                                            .flatMap(channel -> channel.getRestChannel().createMessage("Welcome to the server " + memberMention + " before you're able to fully interact with the server you need to verify your account. Start by entering your student number into the slash command \"/begin <student_number>\"!"))
                                            .subscribe();
                                }))
                        .then();

                //TODO:
                // Check if the user had any other alts and ban them too
                // Add them to the database so they may not rejoin and re-verify
                // The code at activates when a user is banned
                Mono<Void> actOnBan = gateway.on(BanEvent.class, event ->
                        Mono.fromRunnable(() -> {
                            String memberID = event.getUser().getId().asString();
                            String memberName0 = event.getUser().getUsername();

                            //TODO:
                            // Find all accounts verified under the same student ID
                            //
                        })).then();

                //Logic for commands
                //TODO:
                // Carry out the verification process
                // Have good error messages
                // Assign a blacklisted role to anyone who tries to verify with a banned student ID, or maybe just ban them
                // The code that is activated when a message is sent
                // Commands so admins can change the various roles & override
                // Have a help command for users
                // Have a setup help command for admins
                gateway.on(new ReactiveEventAdapter() {
                    @Override
                    public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
                        event.deferReply().subscribe();
                        String result = null;
                        if (event.getCommandName().equals("begin")) {
                            result = "Please check your student email for a verification code, make sure to check your spam as it might've been sent there. Once you have your code, use /verify to finish verifying.";
                            String studentNumber = event.getOption("student_id").get().getValue().get().asString();

                            if (studentNumber.matches("\\d+")) {
                                try {
                                    ResultSet userResults = sqlRunner.selectUser(studentNumber);
                                    String userID;
                                    //Check if a user already exists for this student, insert otherwise, and get their ID
                                    if (!userResults.next()) {
                                        //There are no rows, so we need to create a user
                                        userResults.close();
                                        sqlRunner.insertUser(studentNumber);

                                        //Run the query again to get the user_id
                                        userResults = sqlRunner.selectUser(studentNumber);
                                    }

                                    userID = userResults.getString("user_id");
                                    userResults.close();

                                    //Get or create account for this studentID and discordID combo
                                    String discordID = event.getInteraction().getMember().get().getId().asString();
                                    ResultSet accountResults = sqlRunner.selectAccount(userID, discordID);
                                    String accountID;
                                    //Check if a user already exists for this student, insert otherwise, and get their ID
                                    if (!accountResults.next()) {
                                        //There are no rows, so we need to create a user
                                        accountResults.close();
                                        sqlRunner.insertAccount(userID, discordID);

                                        //Run the query again to get the user_id
                                        accountResults = sqlRunner.selectAccount(userID, discordID);
                                    }

                                    accountID = accountResults.getString("account_id");
                                    accountResults.close();
                                    //Check that there aren't 3 or more verification tokens made within the past 12 hours - discourages spam
                                    int rows = 0;
                                    ResultSet verificationTokens = sqlRunner.selectVerificationTokens(accountID);
                                    while (verificationTokens.next()) {
                                        rows += 1;
                                    }
                                    verificationTokens.close();
                                    if (rows < 3) {
                                        String verificationCode = StringUtilities.getAlphaNumericString(20);
                                        sqlRunner.insertVerificationToken(accountID, verificationCode);
                                        emailSender.sendVerificationEmail(studentNumber, verificationCode);
                                    } else {
                                        result = "You have made too many attempts to begin verification recently, please either verify using an existing token or try again later.";
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    result = "There was an error contacting the database, please try again or contact a server admin for help.";
                                }
                            } else {
                                result = "The student number you entered was incorrect, please try again! If you do not have a student number (e.g. if you are a staff member or alumni) please contact a moderator of this server!";
                            }
                        }
                        if (event.getCommandName().equals("verify")) {
                            result = "You have successfully verified!";
                            //TODO: Check if verification token exists for that account
                            // Enter the user into the verified part of the db
                            // Delete all tokens for that account
                            return event.editReply(result);
                        }
                        if (event.getCommandName().equals("setup")) {
                            result = "You have successfully configured the bot!";

                            //Get inputs
                            String verificationChannel = event.getOption("verification_channel").get().getValue().get().asString();
                            String adminChannel = event.getOption("admin_channel").get().getValue().get().asString();
                            String unverifiedRole = event.getOption("unverified_role").get().getValue().get().asString();
                            String verifiedRole = event.getOption("verified_role").get().getValue().get().asString();

                            //Check if admin
                            AtomicBoolean admin = new AtomicBoolean(false);
                            event.getInteraction()
                                    .getMember()
                                    .get()
                                    .getBasePermissions()
                                    .map(perms -> perms.contains(Permission.ADMINISTRATOR))
                                    .subscribe(hasAdmin -> admin.set(hasAdmin));

                            if (!admin.get()){
                                result = "You don't have permissions to perform this command! You need to be an administrator on the server to do this.";
                                return event.editReply(result);
                            }

                            //Validate the inputs
                            AtomicBoolean verificationChannelValid = new AtomicBoolean(false);
                            AtomicBoolean adminChannelValid = new AtomicBoolean(false);
                            AtomicBoolean unverifiedRoleValid = new AtomicBoolean(false);
                            AtomicBoolean verifiedRoleValid = new AtomicBoolean(false);

                            Snowflake guildSnowflake = event.getInteraction().getGuildId().get();

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
                                    result = "The verification channel ID you entered does not seem to exist in this server. ";
                                }
                                if (!adminChannelValid.get()) {
                                    result = result + "The admin channel ID you entered does not seem to exist in this server. ";
                                }
                                if (!unverifiedRoleValid.get()) {
                                    result = result + "The unverified role ID you entered does not seem to exist in this server. ";
                                }
                                if (!verifiedRoleValid.get()) {
                                    result = result + "The verified role ID you entered does not seem to exist in this server. ";
                                }
                                return event.editReply(result);
                            }

                            //Enter the new configuration into the database
                            try {
                                sqlRunner.updateGuildData(adminChannel, verificationChannel, unverifiedRole, verifiedRole, guildSnowflake.asString());
                            } catch (SQLException e) {
                                e.printStackTrace();
                                result = "Configuring bot failed, please try again or contact the bot admin.";
                                return event.editReply(result);
                            }

                            //Edit the existing GuildData for this server
                            GuildData guildData = guildDataMap.get(guildSnowflake);
                            guildData.setAdminChannelID(Snowflake.of(adminChannel));
                            guildData.setVerificationChannelID(Snowflake.of(verificationChannel));
                            guildData.setUnverifiedRoleID(Snowflake.of(unverifiedRole));
                            guildData.setVerifiedRoleID(Snowflake.of(verifiedRole));
                        }
                        if (event.getCommandName().equals("help")) {
                            result = "To begin verification run /verify! Admins can run /setup to configure the bot!";
                        }
                        return event.editReply(result);
                    }
                }).blockLast();

                // combine them!
                return actOnJoin.and(actOnBan);
            });

            login.block();
        } else {
          System.out.println("You have entered the incorrect amount of command line arguments, please check that your mySQL and Email login is entered correctly.");
          System.exit(1);
        }
    }
}
