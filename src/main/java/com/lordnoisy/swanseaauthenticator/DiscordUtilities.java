package com.lordnoisy.swanseaauthenticator;

import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public class DiscordUtilities {
    public static final String MESSAGE_TOO_LONG = " ... This message was too long.";
    private static int MAX_MESSAGE_SIZE = 2000;

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
        for(int i = 0; i < applicationCommandRequestList.size(); i++) {
            ApplicationCommandRequest current = applicationCommandRequestList.get(i);
            Mono<Void> createGlobalApplicationCommand = gateway.getRestClient().getApplicationId()
                    .flatMap(applicationID -> {
                        return gateway.getRestClient().getApplicationService().createGlobalApplicationCommand(applicationID, current).then();
                    });
            commandsMono = commandsMono.and(createGlobalApplicationCommand);
        }
        return commandsMono;
    }
}
