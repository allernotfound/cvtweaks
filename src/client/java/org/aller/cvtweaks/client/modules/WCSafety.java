package org.aller.cvtweaks.client.modules;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class WCSafety {

    private static boolean wcSafetyEnabled = false;
    private static long lastNonGlobalMessageTime = 0L;
    private static boolean warned = false;

    private static void sendHudMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    public static void init() {
        // Register /wcsafety toggle command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("wcsafety")
                        .executes(ctx -> {
                            if (!PremiumChecker.isPremium()) {
                                sendHudMessage("§cThe WCSafety feature requires premium!");
                                return 0;
                            }

                            wcSafetyEnabled = !wcSafetyEnabled;
                            String status = wcSafetyEnabled ? "§aenabled" : "§cdisabled";
                            MinecraftClient.getInstance().inGameHud.getChatHud()
                                    .addMessage(Text.literal("§eWCSafety " + status));
                            return 1;
                        })
                )
        );

        // handle normal chat
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if (!wcSafetyEnabled) return true;
            // track when you talk in group/private
            if (message.startsWith("/p ") || message.startsWith("/msg ") || message.startsWith("/r ")) {
                lastNonGlobalMessageTime = System.currentTimeMillis();
                warned = false;
            }
            return true;
        });

        // handle commands (starting with /)
        ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (!wcSafetyEnabled) return true;

            // track recent non-global activity
            if (command.startsWith("p ") || command.startsWith("msg ") || command.startsWith("r ")) {
                lastNonGlobalMessageTime = System.currentTimeMillis();
                warned = false;
                return true;
            }

            // if player tries to run /y soon after /p or /msg
            if (command.startsWith("y ")) {
                long now = System.currentTimeMillis();
                if (now - lastNonGlobalMessageTime < 60_000) {
                    if (!warned) {
                        client.inGameHud.getChatHud().addMessage(Text.literal(
                                "§c⚠ You recently chatted in group/private! " +
                                        "Send again to confirm global message."));
                        warned = true;
                        return false; // cancel first send
                    } else {
                        warned = false; // allow second send
                        lastNonGlobalMessageTime = 0;
                        return true;
                    }
                }
            }

            return true; // allow everything else
        });
    }
}
