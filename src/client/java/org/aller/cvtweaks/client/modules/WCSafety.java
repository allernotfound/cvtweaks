package org.aller.cvtweaks.client.modules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WCSafety {

    private static boolean wcSafetyEnabled = true; // default on
    private static long lastNonGlobalMessageTime = 0L;
    private static boolean warned = false;

    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config").resolve("cvtweaks-wcsafety.json");

    private static void saveConfig() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("wcSafetyEnabled", wcSafetyEnabled);
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                JsonObject obj = GSON.fromJson(content, JsonObject.class);
                if (obj.has("wcSafetyEnabled")) {
                    wcSafetyEnabled = obj.get("wcSafetyEnabled").getAsBoolean();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendHudMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    public static void init() {
        loadConfig(); // load saved state at startup

        // Register /wcsafety toggle command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("wcsafety")
                        .executes(ctx -> {
                            if (!PremiumChecker.isPremium()) {
                                sendHudMessage("§cThe WCSafety feature requires premium!");
                                return 0;
                            }

                            wcSafetyEnabled = !wcSafetyEnabled;
                            saveConfig(); // persist change
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
            if (message.startsWith("/p ") || message.startsWith("/msg ") || message.startsWith("/r ")) {
                lastNonGlobalMessageTime = System.currentTimeMillis();
                warned = false;
            }
            return true;
        });

        // handle commands
        ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!wcSafetyEnabled) return true;

            if (command.startsWith("p ") || command.startsWith("msg ") || command.startsWith("r ")) {
                lastNonGlobalMessageTime = System.currentTimeMillis();
                warned = false;
                return true;
            }

            if (command.startsWith("y ")) {
                long now = System.currentTimeMillis();
                if (now - lastNonGlobalMessageTime < 60_000) {
                    if (!warned) {
                        client.inGameHud.getChatHud().addMessage(Text.literal(
                                "§c⚠ You recently chatted in group/private! Send again to confirm global message."));
                        warned = true;
                        return false; // cancel first send
                    } else {
                        warned = false; // allow second send
                        lastNonGlobalMessageTime = 0;
                        return true;
                    }
                }
            }

            return true;
        });
    }
}
