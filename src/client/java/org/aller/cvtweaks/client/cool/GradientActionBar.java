package org.aller.cvtweaks.client.cool;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

public class GradientActionBar {
    private static String currentMessage = "";
    private static long messageStartTime = 0;
    private static final long MESSAGE_DURATION = 8000; // 8 seconds total
    private static final long FADE_DURATION = 2500; // 2.5 seconds fade
    private static final long FULL_OPACITY_DURATION = MESSAGE_DURATION - FADE_DURATION; // 5.5 seconds at full opacity

    public static void init() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            long elapsed = System.currentTimeMillis() - messageStartTime;
            if (!currentMessage.isEmpty() && elapsed < MESSAGE_DURATION) {
                float alpha = calculateAlpha(elapsed);
                renderGradientActionBar(drawContext, alpha);
            }
        });
    }

    public static void show(String message) {
        currentMessage = message;
        messageStartTime = System.currentTimeMillis();
    }

    private static float calculateAlpha(long elapsed) {
        if (elapsed < FULL_OPACITY_DURATION) {
            return 1.0f; // Full opacity for first 5.5 seconds
        } else {
            // Fade out over the last 2.5 seconds
            float fadeProgress = (float)(elapsed - FULL_OPACITY_DURATION) / FADE_DURATION;
            return 1.0f - fadeProgress; // 1.0 -> 0.0
        }
    }

    private static void renderGradientActionBar(DrawContext context, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int textWidth = client.textRenderer.getWidth(currentMessage);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 59;

        drawSmoothGradientText(context, client.textRenderer, currentMessage, x, y, textWidth, alpha);
    }

    private static void drawSmoothGradientText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int textWidth, float alpha) {
        long time = System.currentTimeMillis();
        float offset = (time % 3000) / 3000f;

        // Draw text in segments with interpolated colors for smooth gradient
        int segments = Math.max(text.length(), 10);
        float segmentWidth = (float) textWidth / segments;

        for (int i = 0; i < segments; i++) {
            float progress = (float) i / segments;
            int color = getGradientColor(offset + progress, alpha);

            int startX = x + (int)(i * segmentWidth);
            int endX = x + (int)((i + 1) * segmentWidth);

            context.enableScissor(startX, y - 2, endX, y + 11);
            context.drawText(textRenderer, text, x, y, color, true);
            context.disableScissor();
        }
    }

    private static int getGradientColor(float position, float alpha) {
        position = position % 1.0f;

        float hue;
        if (position < 0.5f) {
            hue = 0.08f + (position * 2) * 0.08f;
        } else {
            hue = 0.16f - ((position - 0.5f) * 2) * 0.08f;
        }

        float saturation = 1.0f;
        float brightness = 1.0f;
        float pulse = (float) Math.sin(System.currentTimeMillis() / 400.0) * 0.15f + 0.85f;
        brightness *= pulse;

        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
        int r = Math.min(255, (int)(((rgb >> 16) & 0xFF) * 1.3f));
        int g = Math.min(255, (int)(((rgb >> 8) & 0xFF) * 1.3f));
        int b = Math.min(255, (int)((rgb & 0xFF) * 1.3f));

        // Apply alpha for fade effect
        int a = (int)(255 * alpha);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}