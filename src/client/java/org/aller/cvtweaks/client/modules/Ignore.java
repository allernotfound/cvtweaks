package org.aller.cvtweaks.client.modules;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Client-side ignore system for global chat messages.
 */
public class Ignore {

    private static final Logger LOGGER = LoggerFactory.getLogger("CVTweaks/Ignore");
    private static final Set<String> ignoredPlayers = new HashSet<>();
    private static final Gson gson = new Gson();
    private static File configFile;

    public static void init() {
        // Initialize config file safely (after MinecraftClient exists)
        configFile = new File(MinecraftClient.getInstance().runDirectory, "cv_tweaks_ignore.json");
        loadConfig();

        // Register /ignore commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("ignore")
                    .then(ClientCommandManager.literal("add")
                            .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                    .executes(ctx -> {
                                        if (!PremiumChecker.isPremium()) {
                                            sendHudMessage("§cThe Ignore feature requires premium!");
                                            return 0;
                                        }
                                        
                                        String player = StringArgumentType.getString(ctx, "player");
                                        ignoredPlayers.add(player);
                                        saveConfig();
                                        sendHudMessage("§aAdded " + player + " to ignore list.");
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("remove")
                            .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                    .executes(ctx -> {
                                        if (!PremiumChecker.isPremium()) {
                                            sendHudMessage("§cThe Ignore feature requires premium!");
                                            return 0;
                                        }
                                        
                                        String player = StringArgumentType.getString(ctx, "player");
                                        ignoredPlayers.remove(player);
                                        saveConfig();
                                        sendHudMessage("§cRemoved " + player + " from ignore list.");
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("list")
                            .executes(ctx -> {
                                if (!PremiumChecker.isPremium()) {
                                    sendHudMessage("§cThe Ignore feature requires premium!");
                                    return 0;
                                }
                                
                                sendHudMessage("§aIgnored players: " + ignoredPlayers);
                                return 1;
                            })
                    )
            );
        });

        // Hook into chat messages and block ignored players
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, profile, params, timestamp) -> {
            return shouldDisplayMessage(message);
        });

        // Also hook into game messages (catches tellraw, announcements, etc.)
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            return shouldDisplayMessage(message);
        });
    }

    // Sends a chat message safely to HUD
    private static void sendHudMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    // Returns true if the message should be displayed
    public static boolean shouldDisplayMessage(Text message) {
        String raw = message.getString();


        // remove weird symbol formatting
        String clean = raw.replaceAll("§.", "");

        for (String ignored : ignoredPlayers) {
            // Match: ]Username( or ]Username: anywhere in the message (case-insensitive)
            String pattern1 = "]" + ignored + "(";
            String pattern2 = "]" + ignored + ":";

            String lowerClean = clean.toLowerCase();

            // prevent messages in local chat
            if (lowerClean.startsWith("<")) {
                return true;
            }

            if (lowerClean.contains(pattern1.toLowerCase()) || lowerClean.contains(pattern2.toLowerCase())) {
                LOGGER.info("BLOCKING message from ignored player: {}", ignored);
                return false; // block message
            }
        }

        return true; // allow message
    }
    // Load ignored players from config
    private static void loadConfig() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            Type setType = new TypeToken<Set<String>>() {}.getType();
            Set<String> loaded = gson.fromJson(reader, setType);
            if (loaded != null) ignoredPlayers.addAll(loaded);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Save ignored players to config
    private static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(ignoredPlayers, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}