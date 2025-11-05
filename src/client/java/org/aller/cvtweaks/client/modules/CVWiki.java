package org.aller.cvtweaks.client.modules;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class CVWiki {

    public static void initialize() {
        registerCommand();
    }

    private static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("cvwiki")
                    .then(ClientCommandManager.argument("text", greedyString())
                            .executes(context -> {
                                String searchText = getString(context, "text");
                                sendWikiLink(context.getSource().getPlayer(), searchText);
                                return 1;
                            })
                    )
                    .executes(context -> {
                        // If no argument provided, show usage
                        context.getSource().getPlayer().sendMessage(
                                Text.literal("§cUsage: /cvwiki <search text>"), false
                        );
                        return 1;
                    })
            );
        });
    }

    private static void sendWikiLink(net.minecraft.client.network.ClientPlayerEntity player, String searchText) {
        try {
            // URL encode the search text to handle spaces and special characters
            String encodedText = URLEncoder.encode(searchText, StandardCharsets.UTF_8);
            String wikiUrl = "https://cubeville.wiki/wiki/?search=" + encodedText;

            // Create the message with colors and clickable link
            MutableText message = Text.literal("Wiki Link: ")
                    .setStyle(Style.EMPTY.withColor(0xFFD700)); // Gold color

            // Create hover text
            Text hoverText = Text.literal("§aClick to open in browser\n§7Search: §f" + searchText);

            MutableText linkText = Text.literal(wikiUrl)
                    .styled(style -> {
                                try {
                                    return style
                                            .withColor(0xFFFFFF) // White color
                                            .withClickEvent(new net.minecraft.text.ClickEvent.OpenUrl(new URI(wikiUrl)))
                                            .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(hoverText));
                                } catch (URISyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );

            // Combine the message
            message.append(linkText);

            // Send the message
            player.sendMessage(message, false);

        } catch (Exception e) {
            // If URL encoding fails, send a simple message
            player.sendMessage(
                    Text.literal("§6Wiki Link: §fhttps://cubeville.wiki/wiki/?search=" + searchText.replace(" ", "+")),
                    false
            );
        }
    }

    // Alternative method for simple non-clickable link
    private static void sendSimpleWikiLink(net.minecraft.client.network.ClientPlayerEntity player, String searchText) {
        try {
            String encodedText = URLEncoder.encode(searchText, StandardCharsets.UTF_8);
            String message = "§6Wiki Link: §fhttps://cubeville.wiki/wiki/?search=" + encodedText;
            player.sendMessage(Text.literal(message), false);
        } catch (Exception e) {
            String message = "§6Wiki Link: §fhttps://cubeville.wiki/wiki/?search=" + searchText.replace(" ", "+");
            player.sendMessage(Text.literal(message), false);
        }
    }

    // Test method
    public static void testWikiCommand(String testText) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            sendWikiLink(client.player, testText);
        }
    }
}