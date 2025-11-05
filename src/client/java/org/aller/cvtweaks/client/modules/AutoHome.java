package org.aller.cvtweaks.client.modules;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * AutoHome module for automatically setting home when health is low.
 */
public class AutoHome {

    private static boolean autoHomeEnabled = false;
    private static final float LOW_HEALTH_THRESHOLD = 6.0f; // 3 hearts (6 half-hearts)
    private static boolean homeCooldown = false;

    private static void sendHudMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    public static void init() {
        // Register /autohome command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("autohome")
                        .then(ClientCommandManager.argument("state", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("on");
                                    builder.suggest("off");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    if (!PremiumChecker.isPremium()) {
                                        sendHudMessage("§cThe AutoHome feature requires premium!");
                                        return 0;
                                    }

                                    String state = StringArgumentType.getString(ctx, "state");
                                    MinecraftClient client = MinecraftClient.getInstance();

                                    if (state.equalsIgnoreCase("on")) {
                                        autoHomeEnabled = true;
                                        homeCooldown = false;
                                        client.inGameHud.getChatHud()
                                                .addMessage(Text.literal("§aAutoHome enabled"));
                                    } else if (state.equalsIgnoreCase("off")) {
                                        autoHomeEnabled = false;
                                        homeCooldown = false;
                                        client.inGameHud.getChatHud()
                                                .addMessage(Text.literal("§cAutoHome disabled"));
                                    } else {
                                        client.inGameHud.getChatHud()
                                                .addMessage(Text.literal("§cUsage: /autohome <on/off>"));
                                    }
                                    return 1;
                                })
                        )
                        .executes(ctx -> {
                            // Toggle if no argument provided
                            autoHomeEnabled = !autoHomeEnabled;
                            homeCooldown = false;

                            String status = autoHomeEnabled ? "§aenabled" : "§cdisabled";
                            MinecraftClient.getInstance().inGameHud.getChatHud()
                                    .addMessage(Text.literal("§eAutoHome " + status));
                            return 1;
                        })
                )
        );

        // Register tick event to check health
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!autoHomeEnabled || homeCooldown) {
                return;
            }

            if (client.player != null && client.getNetworkHandler() != null) {
                float health = client.player.getHealth();

                // Check if health is below threshold
                if (health <= LOW_HEALTH_THRESHOLD && health > 0) {
                    if (!client.world.getRegistryKey().getValue().getPath().contains("creative")) {
                        // Send /sethome command
                        client.getNetworkHandler().sendChatCommand("sethome");

                        // Notify player
                        client.inGameHud.getChatHud()
                                .addMessage(Text.literal("§6Autohome triggered!"));

                        // Disable auto-home and set cooldown
                        autoHomeEnabled = false;
                        homeCooldown = true;
                    }
                }
            }
        });
    }
}