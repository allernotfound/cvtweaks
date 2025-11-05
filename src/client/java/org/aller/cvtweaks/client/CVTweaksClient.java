package org.aller.cvtweaks.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.aller.cvtweaks.client.modules.*;

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

        ChannelToggle.init();
        WCSafety.init();
        Ignore.init();
        AutoHome.init();
        NearestDrop.init();

        // old cvtweaks ported stuff

        RegionScript.initialize();
        CameraShake.initialize();
        CVWiki.initialize();
        FlySpeed.initialize();
        Keybinds.initialize();
    }
}
