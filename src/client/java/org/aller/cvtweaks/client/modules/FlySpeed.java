package org.aller.cvtweaks.client.modules;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class FlySpeed {

    // Key bindings
    private static KeyBinding increaseSpeedKey;
    private static KeyBinding decreaseSpeedKey;

    // Speed settings
    private static final float DEFAULT_FLY_SPEED = 0.05f;
    private static final float MIN_FLY_SPEED = 0.001f;
    private static final float MAX_FLY_SPEED = 1.0f;
    private static final float SPEED_INCREMENT = 0.01f;

    // State tracking
    private static boolean wasIncreasePressed = false;
    private static boolean wasDecreasePressed = false;
    private static long lastSpeedChange = 0;
    private static final long SPEED_CHANGE_COOLDOWN = 100; // milliseconds

    /**
     * Initialize the fly speed controller
     * Call this method during mod initialization
     */
    public static void initialize() {
        // Register key bindings
        increaseSpeedKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Increase Fly Speed",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL, // = key (+ when shifted)
                "CVTweaks"
        ));

        decreaseSpeedKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Decrease Fly Speed",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS, // - key
                "CVTweaks"
        ));

        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(FlySpeed::onClientTick);
    }

    /**
     * Handle client tick events
     */
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        // Only work in creative mode
        if (!client.player.getAbilities().creativeMode || !client.player.getAbilities().flying) {
            return;
        }

        boolean increasePressed = increaseSpeedKey.isPressed();
        boolean decreasePressed = decreaseSpeedKey.isPressed();

        // Check for reset (both keys pressed)
        if (increasePressed && decreasePressed) {
            if (!wasIncreasePressed || !wasDecreasePressed) {
                resetFlySpeed(client);
            }
        }
        // Check for speed increase
        else if (increasePressed && !wasIncreasePressed) {
            increaseFlySpeed(client);
        }
        // Check for speed decrease
        else if (decreasePressed && !wasDecreasePressed) {
            decreaseFlySpeed(client);
        }

        // Update previous state
        wasIncreasePressed = increasePressed;
        wasDecreasePressed = decreasePressed;
    }

    /**
     * Increase the fly speed
     */
    private static void increaseFlySpeed(MinecraftClient client) {
        if (!canChangeSpeed()) {
            return;
        }

        PlayerAbilities abilities = client.player.getAbilities();
        float newSpeed = Math.min(abilities.getFlySpeed() + SPEED_INCREMENT, MAX_FLY_SPEED);

        if (newSpeed != abilities.getFlySpeed()) {
            abilities.setFlySpeed(newSpeed);
            sendSpeedMessage(client, "Fly speed set to: " + String.format("%.3f", newSpeed), "lime");
            lastSpeedChange = System.currentTimeMillis();
        }
    }

    /**
     * Decrease the fly speed
     */
    private static void decreaseFlySpeed(MinecraftClient client) {
        if (!canChangeSpeed()) {
            return;
        }

        PlayerAbilities abilities = client.player.getAbilities();
        float newSpeed = Math.max(abilities.getFlySpeed() - SPEED_INCREMENT, MIN_FLY_SPEED);

        if (newSpeed != abilities.getFlySpeed()) {
            abilities.setFlySpeed(newSpeed);
            sendSpeedMessage(client, "Fly speed set to: " + String.format("%.3f", newSpeed), "red");
            lastSpeedChange = System.currentTimeMillis();
        }
    }

    /**
     * Reset fly speed to default
     */
    private static void resetFlySpeed(MinecraftClient client) {
        if (!canChangeSpeed()) {
            return;
        }

        PlayerAbilities abilities = client.player.getAbilities();

        if (abilities.getFlySpeed() != DEFAULT_FLY_SPEED) {
            abilities.setFlySpeed(DEFAULT_FLY_SPEED);
            sendSpeedMessage(client, "Fly speed reset", "yellow");
            lastSpeedChange = System.currentTimeMillis();
        }
    }

    /**
     * Check if enough time has passed since last speed change
     */
    private static boolean canChangeSpeed() {
        return System.currentTimeMillis() - lastSpeedChange > SPEED_CHANGE_COOLDOWN;
    }

    /**
     * Send a colored speed change message to the player's action bar
     */
    private static void sendSpeedMessage(MinecraftClient client, String message, String color) {
        if (client.player != null) {
            Text coloredMessage;
            switch (color.toLowerCase()) {
                case "lime":
                    coloredMessage = Text.literal(message).styled(style -> style.withColor(0x00FF00));
                    break;
                case "red":
                    coloredMessage = Text.literal(message).styled(style -> style.withColor(0xFF0000));
                    break;
                case "yellow":
                    coloredMessage = Text.literal(message).styled(style -> style.withColor(0xFFFF00));
                    break;
                default:
                    coloredMessage = Text.literal(message);
                    break;
            }
            client.player.sendMessage(coloredMessage, true); // true = actionbar
        }
    }

    /**
     * Get the current fly speed
     */
    public static float getCurrentFlySpeed() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            return client.player.getAbilities().getFlySpeed();
        }
        return DEFAULT_FLY_SPEED;
    }

    /**
     * Set fly speed directly
     */
    public static void setFlySpeed(float speed) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getAbilities().creativeMode) {
            float clampedSpeed = Math.max(MIN_FLY_SPEED, Math.min(speed, MAX_FLY_SPEED));
            client.player.getAbilities().setFlySpeed(clampedSpeed);
        }
    }

    /**
     * Get the default fly speed
     */
    public static float getDefaultFlySpeed() {
        return DEFAULT_FLY_SPEED;
    }

    /**
     * Get the minimum fly speed
     */
    public static float getMinFlySpeed() {
        return MIN_FLY_SPEED;
    }

    /**
     * Get the maximum fly speed
     */
    public static float getMaxFlySpeed() {
        return MAX_FLY_SPEED;
    }
}