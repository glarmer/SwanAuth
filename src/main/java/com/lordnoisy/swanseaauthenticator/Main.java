package com.lordnoisy.swanseaauthenticator;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.MessageCreateMono;
import discord4j.gateway.intent.IntentSet;
import reactor.core.publisher.Mono;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private boolean enabled = true;

    //TODO : I have removed the majority of the code here, this is just a shell, so write all of this
    //TODO : Maybe make a setup tutorial for admins when they invite the bot
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
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }

            DiscordClient client = DiscordClient.create(token);
            Mono<Void> login = DiscordClient.create(token).gateway().setEnabledIntents(IntentSet.all()).withGateway((GatewayDiscordClient gateway) -> {

                //TODO : Get each servers details and enter it into a map
                final Map<Snowflake, GuildData> serverMap = new HashMap<>();
                gateway.getGuilds().doOnEach(guild -> {
                    try {
                        Snowflake guildID = guild.get().getId();
                        //TODO: Get the server info from the mySQL database, if there is none create the empty db stuff
                        // Check if table for server exists
                        // if it exists get the info, make a new GuildData with the info
                        // if it doesn't exist make the table and db entries (although it will be mostly empty) and make a new GuildData
                        // TODO:
                        // serverMap.put(guildID, guildData);
                    } catch (NullPointerException npe) {
                        System.out.println("Continuing");
                    }
                }).then().subscribe();

                client.gateway().setEnabledIntents(IntentSet.all());


                //TODO : Error handling for is default role is not set, essentially make it do nothing.
                Mono<Void> actOnJoin = gateway.on(MemberJoinEvent.class, event ->
                                Mono.fromRunnable(() -> {
                                    Snowflake serverID = event.getGuildId();
                                    GuildData guildData = serverMap.get(serverID);
                                    Snowflake defaultRoleID = guildData.getDefaultRoleID();
                                    Snowflake messageChannelID = guildData.getMessageChannelID();
                                    Member member = event.getMember();
                                    String memberMention = "<@" + member.getId().asString() + ">";

                                    member.addRole(defaultRoleID, "Assign default role on join");

                                    gateway.getChannelById(messageChannelID)
                                            .flatMap(channel -> channel.getRestChannel().createMessage("Welcome to the server " + memberMention + " before you're able to fully interact with the server you need to verify your account. Start by replying to this message with \"$verify <your_student_number>\""))
                                            .subscribe();
                                }))
                        .then();

                //TODO:
                // Check if the user had any other alts and ban them too
                // Add them to the database so they may not rejoin
                // The code at activates when a user is banned
                Mono<Void> actOnBan = gateway.on(BanEvent.class, event ->
                        Mono.fromRunnable(() -> {
                            String memberID = event.getUser().getId().asString();
                            String memberName0 = event.getUser().getUsername();
                            try {
                                ArrayList<String> toBlacklist = SQLRunner.blackListUser(connection, memberID);
                                for (int i = 0; i < toBlacklist.size(); i++) {
                                    try {
                                        String memberName = event.getGuild().block().getMemberById(Snowflake.of(toBlacklist.get(i))).block().getUsername();
                                        event.getGuild().block().getMemberById(Snowflake.of(toBlacklist.get(i))).block().addRole(Snowflake.of(blacklistedRoleID)).subscribe();
                                        event.getGuild().block().getChannelById(Snowflake.of(adminChannel)).block().getRestChannel().createMessage("Blacklisted <@" + toBlacklist.get(i) + "> : " + memberName).subscribe();
                                        event.getGuild().block().ban(Snowflake.of(toBlacklist.get(i))).block();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                event.getGuild().block().getChannelById(Snowflake.of(adminChannel)).block().getRestChannel().createMessage("Blacklisted <@" + memberID + "> : " + memberName0).subscribe();

                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("It is likely this user did not verify.");
                            }
                        })).then();

                //TODO:
                // Carry out the verification process
                // Have good error messages
                // Assign a blacklisted role to anyone who tries to verify with a banned student ID, or maybe just ban them
                // The code that is activated when a message is sent
                // Commands so admins can change the various roles & override
                // Have a help command for users
                // Have a setup help command for admins
                Mono<Void> handleCommands = gateway.on(MessageCreateEvent.class, event -> {
                    System.out.println("Noticed message");
                    final Message message = event.getMessage();
                    String discordID = message.getAuthor().get().getId().asString();
                    String str = message.getContent();
                    //add space between . and command
                    if (str.startsWith("$")) {

                    }
                    return Mono.empty();
                }).then();

                // combine them!
                return actOnJoin.and(handleCommands).and(actOnBan);
            });

            login.block();
        } else {
          System.out.println("You have entered the incorrect amount of command line arguments, please check that your mySQL and Email login is entered correctly.");
          System.exit(1);
        }
    }
}
