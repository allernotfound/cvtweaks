package org.aller.cvtweaks.client.modules;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class CameraShake {

    private static float shakeIntensity = 0f;
    private static int shakeDuration = 0;
    private static int shakeTimer = 0;
    private static final Random random = Random.create();

    public static void initialize() {
        // Register command
//        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
//            registerCommand(dispatcher);
//        });

        // Register tick event to apply shake
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && shakeTimer > 0) {
                applyShake(client);
                shakeTimer--;
            }
        });
    }

//    private static void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
//        dispatcher.register(literal("camerashake")
//                .then(argument("intensity", FloatArgumentType.floatArg(0.1f, 10.0f))
//                        .then(argument("duration", IntegerArgumentType.integer(1, 200))
//                                .executes(CameraShake::executeShake)
//                        )
//                )
//        );
//    }

    public static int executeShake(float intensity, int duration) {

        // Start shake
        shakeIntensity = intensity;
        shakeDuration = duration;
        shakeTimer = duration;

        return 1;
    }

    private static void applyShake(MinecraftClient client) {
        if (client.player == null) return;

        // Calculate current shake strength (fades over time)
        float fadeProgress = (float) shakeTimer / shakeDuration;
        float currentIntensity = shakeIntensity * fadeProgress * fadeProgress;

        // Generate random shake offsets
        float yawShake = (random.nextFloat() - 0.5f) * 2 * currentIntensity;
        float pitchShake = (random.nextFloat() - 0.5f) * 2 * currentIntensity;

        // Apply shake by modifying player rotation
        client.player.setYaw(client.player.getYaw() + yawShake);
        client.player.setPitch(client.player.getPitch() + pitchShake);

        // Clamp pitch to prevent camera flipping
        float pitch = client.player.getPitch();
        if (pitch > 90f) client.player.setPitch(90f);
        if (pitch < -90f) client.player.setPitch(-90f);
    }
}