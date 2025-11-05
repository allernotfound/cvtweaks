package org.aller.cvtweaks.client.modules;


import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static KeyBinding openWarpMenu;
    public static KeyBinding toggleGamemode;

    public static void initialize() {
        register();
    }

    private static void sendHudMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    public static void register() {
        openWarpMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open Warp Menu", // The translation key for the keybind's name
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, // The H key
                "CVTweaks" // The translation key for the category
        ));
        toggleGamemode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Toggle Gamemode", // The translation key for the keybind's name
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y, // The H key (not)
                "CVTweaks" // The translation key for the category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openWarpMenu.wasPressed()) {
                if (client.player != null && client.world != null) {
                    if (client.world.getRegistryKey().getValue().getPath().contains("creative")) {
                        if (!PremiumChecker.isPremium()) {
                            sendHudMessage("§cThe warp menu feature requires premium!");
                            return;
                        }

                        client.setScreen(new WarpMenuScreen());
                    }
                }
            };
            while (toggleGamemode.wasPressed()) {
                if (client.player != null && client.world != null) {
                    if (client.world.getRegistryKey().getValue().getPath().contains("creative")) {
                        client.player.networkHandler.sendChatCommand("toggle gamemode");
                    }
                }
            }
        });
    }
}
