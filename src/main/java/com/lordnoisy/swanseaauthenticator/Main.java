package com.lordnoisy.swanseaauthenticator;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.IntentSet;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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

                ApplicationCommandRequest studentCommand = ApplicationCommandRequest.builder()
                        .name("student")
                        .description("Begin the verification process by entering your Student ID.")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("student_id")
                                .description("Your Student ID")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                ApplicationCommandRequest verifyCommand = ApplicationCommandRequest.builder()
                        .name("verify")
                        .description("Begin the verification process by entering your Student ID.")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("verification_code")
                                .description("The verification code you received via email!")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                List<ApplicationCommandRequest> applicationCommandRequestList = List.of(studentCommand, verifyCommand);

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
                                            .flatMap(channel -> channel.getRestChannel().createMessage("Welcome to the server " + memberMention + " before you're able to fully interact with the server you need to verify your account. Start by replying to this message with \"$verify <your_student_number>\""))
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
                        if (event.getCommandName().equals("student")) {
                            String result = "Please check your student email for a verification code! When ready use /verify to finish verifying.";
                            return event.reply(result);
                        }
                        if (event.getCommandName().equals("verify")) {
                            String result = "d";
                            return event.reply(result);
                        }
                        return Mono.empty();
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
