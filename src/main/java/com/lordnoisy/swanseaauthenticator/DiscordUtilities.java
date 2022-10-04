package com.lordnoisy.swanseaauthenticator;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.lordnoisy.swanseaauthenticator.Main.EMBED_COLOUR;
import static com.lordnoisy.swanseaauthenticator.Main.FOOTER_ICON_URL;

public class DiscordUtilities {
    public static final String MESSAGE_TOO_LONG = " ... This message was too long.";
    private static final int MAX_MESSAGE_SIZE = 2000;

    public static String truncateMessage(String message) {
        if (message.length() > MAX_MESSAGE_SIZE) {
            message = message.substring(0, 1980).concat(MESSAGE_TOO_LONG);
        }
        return message;
    }

    public static String getMention(String memberID) {
        return "<@" + memberID + ">";
    }

    public static Mono<Void> createGlobalCommandsMono(List<ApplicationCommandRequest> applicationCommandRequestList, GatewayDiscordClient gateway) {
        Mono<Void> commandsMono = Mono.empty();
        for (int i = 0; i < applicationCommandRequestList.size(); i++) {
            ApplicationCommandRequest current = applicationCommandRequestList.get(i);
            Mono<Void> createGlobalApplicationCommand = gateway.getRestClient().getApplicationId().flatMap(applicationID -> {
                return gateway.getRestClient().getApplicationService().createGlobalApplicationCommand(applicationID, current).then();
            });
            commandsMono = commandsMono.and(createGlobalApplicationCommand);
        }
        return commandsMono;
    }

    public static EmbedCreateSpec loggingEmbed(String memberID, String beginOrFinal, String result) {
        String memberMention = DiscordUtilities.getMention(memberID);
        String title;
        String description = "The user in question is: ";
        result = "\"".concat(result).concat("\"");
        if (beginOrFinal.equals("BEGIN")) {
            title = "A user has begun verification!";
        } else {
            title = "A user has attempted to finalise their verification!";
        }
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(title)
                .color(EMBED_COLOUR)
                .description(description + memberMention + "\n\nThe message they received in return was: " + result)
                .footer(EmbedCreateFields.Footer.of("SwanAuth | " + StringUtilities.getDateTime(), FOOTER_ICON_URL))
                .build();
        return embed;
    }

    public static Mono<Void> getVerificationLoggingMono(GuildData guildData, String memberID, String result, GatewayDiscordClient gateway, String beginOrFinal) {
        Mono<Void> sendAdminMessage = Mono.empty();
        if (guildData.getVerificationLogging().equals("ENABLED")) {
            EmbedCreateSpec embed = DiscordUtilities.loggingEmbed(memberID, beginOrFinal, result);
            sendAdminMessage = gateway.getChannelById(guildData.getAdminChannelID())
                    .ofType(GuildMessageChannel.class)
                    .flatMap(channel -> channel.createMessage(embed))
                    .then();
        }
        return sendAdminMessage;
    }
}
