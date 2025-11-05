package org.aller.cvtweaks.client.modules;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PopupScreen extends Screen {
    private final String message;
    private final int popupWidth;
    private final int popupHeight;
    private final int padding = 20;
    private final int buttonHeight = 20;
    private final int buttonWidth = 60;

    // Add these as instance fields
    private int popupX;
    private int popupY;

    public PopupScreen(String message) {
        super(Text.literal("Popup"));
        this.message = message.replace('%', '§'); // handle color codes here

        // Estimate popup width from text length, with min/max
        int textWidth = Math.min(400, Math.max(200, this.message.length() * 6));
        this.popupWidth = textWidth + (padding * 2);
        this.popupHeight = 80 + (padding * 2);
    }

    @Override
    protected void init() {
        popupX = (this.width - popupWidth) / 2;  // Assign to instance fields
        popupY = (this.height - popupHeight) / 2;

        int buttonX = popupX + (popupWidth - buttonWidth) / 2;
        int buttonY = popupY + popupHeight - buttonHeight - padding;

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Close"), b -> close())
                        .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        // Popup background
        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF2D2D30); // Dark gray
        drawBorder(context, popupX, popupY, popupWidth, popupHeight, 0xFF007ACC);

        // Draw widgets (button)
        super.render(context, mouseX, mouseY, delta);

        // Draw message text last, so it's above everything
        drawCenteredWrappedText(context, message, popupX, popupY + padding + 10, popupWidth - padding * 2, 0xFFFFFF);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 2, color);
        ctx.fill(x, y + h - 2, x + w, y + h, color);
        ctx.fill(x, y, x + 2, y + h, color);
        ctx.fill(x + w - 2, y, x + w, y + h, color);
    }

    private void drawCenteredWrappedText(DrawContext ctx, String text, int startX, int startY, int maxWidth, int color) {
        List<String> lines = wrapText(text, maxWidth);
        int y = startY;
        for (String line : lines) {
            int lineWidth = textRenderer.getWidth(line);
            int x = startX + (popupWidth - lineWidth) / 2;
            ctx.drawTextWithShadow(textRenderer, Text.literal(line), x, y, color);
            y += 12; // line height
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (textRenderer.getWidth(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                if (current.length() > 0) lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}