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
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static final Logger LOG = Loggers.getLogger(GuildCommandRegistrar.class);

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
                                    Snowflake verificationChannelID = guildData.getVerificationChannelID();
                                    if (unverifiedRoleID == null || verificationChannelID == null) {
                                        return Mono.empty();
                                    } else {
                                        Member member = event.getMember();
                                        String memberMention = "<@" + member.getId().asString() + ">";
                                        Mono<Void> addDefaultRoleOnJoin = member.addRole(unverifiedRoleID, "Assign default role on join").then();
                                        Mono<Void> sendWelcomeMessageOnJoin = gateway.getChannelById(verificationChannelID)
                                                .flatMap(channel -> channel.getRestChannel().createMessage("Welcome to the server " + memberMention + " before you're able to fully interact with the server you need to verify your account. Start by entering your student number into the slash command \"/begin <student_number>\"!")).then();
                                        return addDefaultRoleOnJoin.and(sendWelcomeMessageOnJoin);
                                    }
                }).then();

                //TODO: Test this code works
                Mono<Void> actOnBan = gateway.on(BanEvent.class, event ->
                        Mono.fromRunnable(() -> {
                            String memberID = event.getUser().getId().asString();
                            Account account = sqlRunner.getAccountFromDiscordID(memberID);

                            //Account will be null if the user never began verification, so we can't do anything
                            if (!(account == null)) {
                                String userID = account.getUserID();
                                ArrayList<Account> accounts = sqlRunner.getAccountsFromUserID(userID);

                                //Ban any alts
                                for (int i = 0; i < accounts.size(); i++) {
                                    Account currentAccount = accounts.get(i);
                                    //Ensure we're not trying to ban the account that was just banned
                                    if (!currentAccount.getDiscordID().equals(memberID)) {
                                        event.getGuild().map(guild -> guild.ban(Snowflake.of(currentAccount.getDiscordID()))).subscribe();
                                    }

                                    //Insert bans into db
                                    sqlRunner.insertBan(currentAccount.getUserID(), event.getGuildId().asString());
                                }
                            }

                        })).then();

                //Logic for commands
                //TODO:
                // Admin commands? /manualVerify (even though they could just add the role, it makes it more obvious)
                // Have a help command for users
                // Have a setup help command for admins
                // If a banned user tried to verify, ban the account they're trying to verify
                //TODO : Some logic for unbanning - is there an unban event? - but also allow through command
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

                        if (event.getCommandName().equals("begin")) {
                            if (isServerConfigured) {

                                result = "Please check your student email for a verification code, make sure to check your spam as it might've been sent there. Once you have your code, use /verify to finish verifying.";
                                String studentNumber = event.getOption("student_id").get().getValue().get().asString();

                                if (studentNumber.matches("\\d+")) {
                                    //TODO: handle error if accountID is null
                                    //TODO: handle error if userID or accountID is null, or rows if -1
                                    String userID = sqlRunner.getOrCreateUserIDFromStudentID(studentNumber);
                                    String accountID = sqlRunner.getOrCreateAccountIDFromDiscordIDAndUserID(userID, discordID);
                                    if (!sqlRunner.isVerified(accountID, guildSnowflake.asString())) {
                                        //Check that there aren't 3 or more verification tokens made within the past 12 hours - discourages spam
                                        int rows = sqlRunner.selectRecentVerificationTokens(accountID, guildSnowflake.asString());
                                        if (rows < 3) {
                                            String verificationCode = StringUtilities.getAlphaNumericString(20);
                                            sqlRunner.insertVerificationToken(accountID, guildSnowflake.asString(), verificationCode);
                                            emailSender.sendVerificationEmail(studentNumber, verificationCode, guildName.get());
                                        } else {
                                            result = "You have made too many attempts to begin verification recently, please either verify using an existing token or try again later.";
                                        }
                                    } else {
                                        result = "This discord account is already verified on this server!";
                                    }
                                } else {
                                    result = "The student number you entered was incorrect, please try again! If you do not have a student number (e.g. if you are a staff member or alumni) please contact a moderator of this server!";
                                }
                            } else {
                                result = "The server admins haven't configured the bot yet, contact them for assistance.";
                            }
                        }
                        if (event.getCommandName().equals("verify")) {
                            String tokenInput = event.getOption("verification_code").get().getValue().get().asString();
                            if (isServerConfigured) {
                                result = "You have successfully verified!";
                                //TODO: handle error if accountID is null, rows if -1
                                String accountID = sqlRunner.getAccountFromDiscordID(discordID).getAccountID();
                                if (!sqlRunner.isVerified(accountID, guildSnowflake.asString())) {
                                    int rows  = sqlRunner.selectVerificationToken(accountID, guildSnowflake.asString(), tokenInput);
                                    if (rows > 0) {
                                        //TODO: Error handling
                                        String guildID = event.getInteraction().getGuildId().get().asString();
                                        sqlRunner.insertVerification(accountID, guildID);
                                        sqlRunner.deleteVerificationTokens(accountID, guildID);
                                    } else {
                                        result = "The verification token you entered is incorrect, please try again...";
                                    }
                                    event.getInteraction().getMember().map(member -> member.addRole(guildDataMap.get(guildSnowflake).getVerifiedRoleID())).get().subscribe();
                                } else {
                                    result = "This discord account is already verified on this server!";
                                }
                            } else {
                                result = "The server admins haven't configured the bot yet, contact them for assistance.";
                            }
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
                                    .subscribe(admin::set);

                            if (!admin.get()){
                                result = "You don't have permissions to perform this command! You need to be an administrator on the server to do this.";
                                return event.editReply(result);
                            }

                            //Validate the inputs
                            AtomicBoolean verificationChannelValid = new AtomicBoolean(false);
                            AtomicBoolean adminChannelValid = new AtomicBoolean(false);
                            AtomicBoolean unverifiedRoleValid = new AtomicBoolean(false);
                            AtomicBoolean verifiedRoleValid = new AtomicBoolean(false);


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
                            if (!sqlRunner.updateGuildData(adminChannel, verificationChannel, unverifiedRole, verifiedRole, guildSnowflake.asString())) {
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
                }).then();

                // combine them!
                return doOnEachGuild.and(actOnJoin).and(actOnBan).and(actOnSlashCommand);
            });

            login.block();
        } else {
          System.out.println("You have entered the incorrect amount of command line arguments, please check that your mySQL and Email login is entered correctly.");
          System.exit(1);
        }
    }
}
