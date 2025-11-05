package org.aller.cvtweaks.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.stream.Collectors;

public class WarpMenuScreen extends Screen {
    private TextFieldWidget usernameField;
    private List<String> onlineUsernames = List.of();
    private List<String> suggestions = List.of();
    private int selectedSuggestion = -1;

    public WarpMenuScreen() {
        super(Text.literal("Warp Menu"));
    }

    @Override
    protected void init() {
        updateOnlineUsers();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Username input
        usernameField = new TextFieldWidget(
                textRenderer,
                centerX - 100, centerY - 30,
                200, 20,
                Text.literal("Username")
        );
        usernameField.setMaxLength(16);
        usernameField.setPlaceholder(Text.literal("Enter username...").formatted(Formatting.GRAY));
        usernameField.setChangedListener(this::updateSuggestions);
        usernameField.setFocused(true);
        setFocused(usernameField);  // <-- ensures keyboard events go here
        setInitialFocus(usernameField); // keeps consistency

        addDrawableChild(usernameField);

        // Warp button
//        addDrawableChild(
//                ButtonWidget.builder(Text.literal("Warp"), b -> executeWarp())
//                        .dimensions(centerX - 50, centerY + 10, 100, 20)
//                        .build()
//        );

        setInitialFocus(usernameField);
    }

    private void updateOnlineUsers() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            onlineUsernames = client.getNetworkHandler().getPlayerList()
                    .stream()
                    .map(PlayerListEntry::getProfile)
                    .map(p -> p.getName())
                    .collect(Collectors.toList());
        }
    }

    private void updateSuggestions(String text) {
        updateOnlineUsers(); // <-- refresh current player list

        if (text.isEmpty()) {
            suggestions = List.of();
        } else {
            suggestions = onlineUsernames.stream()
                    .filter(name -> name.toLowerCase().startsWith(text.toLowerCase()))
                    .limit(5)
                    .collect(Collectors.toList());
        }
        selectedSuggestion = -1;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Title
        Text title = Text.literal("Warp Menu").formatted(Formatting.WHITE, Formatting.BOLD);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 60, 0xFFFFFF);

        // Suggestions
        if (!suggestions.isEmpty() && usernameField.isFocused()) {
            renderSuggestions(context, mouseX, mouseY);
        }
    }

    private void renderSuggestions(DrawContext context, int mouseX, int mouseY) {
        int fieldX = usernameField.getX();
        int fieldY = usernameField.getY() + usernameField.getHeight() + 2;
        int width = usernameField.getWidth();
        int max = Math.min(suggestions.size(), 5);
        int bgHeight = max * 12 + 4;

        context.fill(fieldX, fieldY, fieldX + width, fieldY + bgHeight, 0xFF000000);
        context.drawBorder(fieldX, fieldY, width, bgHeight, 0xFF555555);

        for (int i = 0; i < max; i++) {
            String s = suggestions.get(i);
            int y = fieldY + 2 + i * 12;
            boolean hovered = mouseX >= fieldX && mouseX <= fieldX + width && mouseY >= y && mouseY <= y + 12;
            boolean selected = i == selectedSuggestion;

            if (hovered || selected) {
                context.fill(fieldX + 1, y, fieldX + width - 1, y + 12, 0xFF333333);
            }
            context.drawText(textRenderer, s, fieldX + 4, y + 2, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!suggestions.isEmpty() && usernameField.isFocused()) {
            int fieldX = usernameField.getX();
            int fieldY = usernameField.getY() + usernameField.getHeight() + 2;
            int width = usernameField.getWidth();
            int max = Math.min(suggestions.size(), 5);

            if (mouseX >= fieldX && mouseX <= fieldX + width) {
                for (int i = 0; i < max; i++) {
                    int y = fieldY + 2 + i * 12;
                    if (mouseY >= y && mouseY <= y + 12) {
                        usernameField.setText(suggestions.get(i));
                        suggestions = List.of();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!suggestions.isEmpty() && usernameField.isFocused()) {
            switch (keyCode) {
                case 264 -> { // Down
                    selectedSuggestion = Math.min(selectedSuggestion + 1, suggestions.size() - 1);
                    if (selectedSuggestion == -1) selectedSuggestion = 0;
                    return true;
                }
                case 265 -> { // Up
                    selectedSuggestion = Math.max(selectedSuggestion - 1, -1);
                    return true;
                }
                case 257, 335 -> { // Enter
                    if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                        usernameField.setText(suggestions.get(selectedSuggestion));
                        suggestions = List.of();
                    } else {
                        executeWarp();
                    }
                    return true;
                }
                case 258 -> { // Tab
                    usernameField.setText(suggestions.get(0));
                    suggestions = List.of();
                    return true;
                }
            }
        }

        if (keyCode == 257 || keyCode == 335) { // Enter
            executeWarp();
            return true;
        }
        if (keyCode == 256) { // Escape
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void executeWarp() {
        String username = usernameField.getText().trim();
        if (!username.isEmpty() && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand("home " + username);
        }
        close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
