package org.aller.cvtweaks.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.aller.cvtweaks.client.cool.GradientActionBar;
import org.aller.cvtweaks.client.modules.*;
import org.lwjgl.glfw.GLFW;

public class CVTweaksClient implements ClientModInitializer {
    private static void sendHudMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    @Override
    public void onInitializeClient() {
        // iniitalizes the premium checker (why do i bother adding comments)
        PremiumChecker.init();
        AutoUpdater.init();

        ChannelToggle.init();
        WCSafety.init();
        Ignore.init();
        AutoHome.init();
        NearestDrop.init();
        ScamBase.init();

        // old cvtweaks ported stuff

        RegionScript.initialize();
        CameraShake.initialize();
        CVWiki.initialize();
        FlySpeed.initialize();
        Keybinds.initialize();

        // chat stuff
        GradientActionBar.init();
    }
}
