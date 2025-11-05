package org.aller.cvtweaks.client.modules;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * ChannelToggle module for toggling chat prefixes (/p or /y).
 */
public class ChannelToggle {

    private static boolean groupChatEnabled = false;
    private static boolean globalChatEnabled = false;

    private static void sendHudMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    public static void init() {
        // Register /group command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("group")
                        .executes(ctx -> {
                            if (!PremiumChecker.isPremium()) {
                                sendHudMessage("§cThe channel toggle feature requires premium!");
                                return 0;
                            }

                            groupChatEnabled = !groupChatEnabled;

                            // Disable the other mode if needed
                            if (groupChatEnabled && globalChatEnabled) {
                                globalChatEnabled = false;
                            }

                            String status = groupChatEnabled ? "enabled" : "disabled";
                            MinecraftClient.getInstance().inGameHud.getChatHud()
                                    .addMessage(Text.literal("§eGroup chat " + status));
                            return 1;
                        })
                )
        );

        // Register /global command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("global")
                        .executes(ctx -> {
                            if (!PremiumChecker.isPremium()) {
                                sendHudMessage("§cThe channel toggle feature requires premium!");
                                return 0;
                            }

                            globalChatEnabled = !globalChatEnabled;

                            // Disable the other mode if needed
                            if (globalChatEnabled && groupChatEnabled) {
                                groupChatEnabled = false;
                            }

                            String status = globalChatEnabled ? "enabled" : "disabled";
                            MinecraftClient.getInstance().inGameHud.getChatHud()
                                    .addMessage(Text.literal("§eGlobal chat " + status));
                            return 1;
                        })
                )
        );

        // Hook into message sending
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            // Don't touch commands or already-prefixed messages
            if (message.startsWith("/")) {
                return true;
            }

            if (groupChatEnabled && !message.startsWith("/p ")) {
                // Send as /p command
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand("p " + message);
                }
                return false;
            }

            if (globalChatEnabled && !message.startsWith("/y ")) {
                // Send as /y command
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand("y " + message);
                }
                return false;
            }

            return true; // Normal message if neither mode is active
        });
    }
}
