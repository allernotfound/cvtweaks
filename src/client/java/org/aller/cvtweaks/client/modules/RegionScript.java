package org.aller.cvtweaks.client.modules;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import javazoom.jl.player.Player;
import net.minecraft.client.gui.screen.Screen;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.DeathScreen;

public class RegionScript {

    private static final List<ActiveSong> activeSongs = new ArrayList<>();
    private static final Map<String, CommandHandler> commands = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static volatile boolean timerRunning = false;
    private static volatile long timerEndTime = 0;
    private static volatile ScheduledFuture<?> timerTask = null; // Track timer task for cancellation

    // Animation tracking
    private static final Map<String, ScheduledFuture<?>> activeAnimations = new HashMap<>();
    private static final double DEFAULT_ANIMATION_SPEED = 0.5; // 500ms between frames

    // Toggle settings - all default to false/off
    private static final Map<String, Boolean> toggleSettings = new HashMap<>();

    private static boolean deathScreenWasOpen = false;

    public static void initialize() {
        // Initialize toggle settings with default values (all off)
        initializeToggleSettings();

        // Register all commands
        registerCommands();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("stopall")
                            .executes(context -> {
                                // Your command logic here
                                stopAllSongs();
                                sendClientMessage("All currently playing songs have been stopped");
                                return 1; // Success
                            })
            );
        });

        // Use ALLOW_GAME event with high priority to intercept messages before they show
        ClientReceiveMessageEvents.ALLOW_GAME.register(Event.DEFAULT_PHASE, (message, overlay) -> {
            String msg = message.getString();

            // Check if RegionScript message contains any of our commands
            if (containsCommand(msg)) {
                if (msg.startsWith("<RG>")) {
                    processCommandChain(msg);
                    return false; // Cancel the message from showing in chat
                }
            }
            return true; // Let normal messages through
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean deathScreenIsOpen = client.currentScreen instanceof DeathScreen;

            if (deathScreenIsOpen && !deathScreenWasOpen) {
                // Player just died
                System.out.println("Player died!");
                onPlayerDeath();
            }

            deathScreenWasOpen = deathScreenIsOpen;
        });
    }

    private static void onPlayerDeath() {
        stopTimer();
    }

    private static void initializeToggleSettings() {
        // Initialize all toggle settings to false (off) by default
        toggleSettings.put("timer_death", false);
        // Add more toggle settings here in the future if needed
    }

    private static void registerCommands() {
        // Register existing commands
        commands.put("playsong", new CommandHandler(1, RegionScript::handlePlaySong));
        commands.put("stopall", new CommandHandler(0, RegionScript::handleStopAll));

        // Register new commands - easy to add more!
        commands.put("showtitle", new CommandHandler(1, RegionScript::handleShowTitle));
        commands.put("showsubtitle", new CommandHandler(1, RegionScript::handleShowSubtitle));
        commands.put("showactionbar", new CommandHandler(1, RegionScript::handleShowActionBar));

        commands.put("title", new CommandHandler(1, RegionScript::handleShowTitle));
        commands.put("sst", new CommandHandler(1, RegionScript::handleShowSubtitle));
        commands.put("sab", new CommandHandler(1, RegionScript::handleShowActionBar));
        commands.put("gotohome", new CommandHandler(1, RegionScript::handleGoToHome));
        commands.put("showtimer", new CommandHandler(2, RegionScript::handleShowTimer));

        // Add the new commands
        commands.put("kill", new CommandHandler(0, RegionScript::handleKill));
        commands.put("stoptimer", new CommandHandler(0, RegionScript::handleStopTimer));
        commands.put("toggle", new CommandHandler(2, RegionScript::handleToggle));
        commands.put("shake", new CommandHandler(2, RegionScript::handleShake));
        commands.put("popup", new CommandHandler(1, RegionScript::handlePopup));
    }

    private static boolean containsCommand(String msg) {
        String lower = msg.toLowerCase(Locale.ROOT);

        for (String commandName : commands.keySet()) {
            String commandPrefix = "!" + commandName;
            if (lower.indexOf(commandPrefix) != -1) {
                return true;
            }
        }
        return false;
    }

    private static void processCommandChain(String msg) {
        // Split the message by | to get individual command segments
        String[] segments = msg.split("\\|");

        long cumulativeDelay = 0; // Track total delay in milliseconds

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();

            // Check if RegionScript segment contains a command
            if (containsCommand(segment)) {
                // Schedule RegionScript command to execute after the cumulative delay
                scheduleCommand(segment, cumulativeDelay);
            } else {
                // RegionScript segment might be a delay value
                try {
                    double delaySeconds = Double.parseDouble(segment);
                    long delayMs = (long) (delaySeconds * 1000);
                    cumulativeDelay += delayMs;
                } catch (NumberFormatException e) {
                    // Not a valid number, ignore RegionScript segment
                    sendClientMessage("Invalid delay value: " + segment);
                }
            }
        }
    }

    private static void scheduleCommand(String commandSegment, long delayMs) {
        scheduler.schedule(() -> {
            processCommand(commandSegment);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private static void processCommand(String msg) {
        String lower = msg.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, CommandHandler> entry : commands.entrySet()) {
            String commandName = entry.getKey();
            CommandHandler handler = entry.getValue();
            String commandPrefix = "!" + commandName;

            int commandIndex = lower.indexOf(commandPrefix);
            if (commandIndex != -1) {
                // Extract arguments
                int argsStart = commandIndex + commandPrefix.length();
                String argsString = "";

                if (argsStart < msg.length()) {
                    argsString = msg.substring(argsStart).trim();
                }

                List<String> args = parseArguments(argsString);

                // Validate argument count
                if (handler.minArgs > 0 && args.isEmpty()) {
                    sendClientMessage("Usage: !" + commandName + " requires at least " + handler.minArgs + " argument(s)");
                    return;
                }

                // Execute command
                CommandContext context = new CommandContext(commandName, args, msg);
                handler.executor.accept(context);
                return; // Only process first matching command
            }
        }
    }

    private static List<String> parseArguments(String argsString) {
        if (argsString.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);

            if (c == '"' && (i == 0 || argsString.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (currentArg.length() > 0) {
                    args.add(currentArg.toString());
                    currentArg = new StringBuilder();
                }
            } else {
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }

        return args;
    }

    // Animation helper methods
    private static boolean isAnimationText(String text) {
        return text.startsWith("$anim ") || text.startsWith("$glitchanim ");
    }

    private static boolean isGlitchAnimation(String text) {
        return text.startsWith("$glitchanim ");
    }

    private static AnimationData parseAnimationData(String text) {
        if (!isAnimationText(text)) {
            return new AnimationData(Arrays.asList(text), DEFAULT_ANIMATION_SPEED);
        }

        if (isGlitchAnimation(text)) {
            return parseGlitchAnimationData(text);
        }

        String animationData = text.substring(6); // Remove "$anim "

        // Check if there's a delay parameter at the end
        String[] parts = animationData.split(" ");
        double delay = DEFAULT_ANIMATION_SPEED;
        String frameData = animationData;

        if (parts.length >= 2) {
            try {
                // Try to parse the last part as a delay
                delay = Double.parseDouble(parts[parts.length - 1]);
                // Remove the delay from the frame data
                frameData = animationData.substring(0, animationData.lastIndexOf(" " + parts[parts.length - 1]));
            } catch (NumberFormatException e) {
                // Last part wasn't a number, use default delay
            }
        }

        String[] frames = frameData.split(",");
        List<String> frameList = new ArrayList<>();
        for (String frame : frames) {
            frameList.add(frame.trim());
        }

        return new AnimationData(frameList, delay);
    }

    private static AnimationData parseGlitchAnimationData(String text) {
        String animationData = text.substring(12); // Remove "$glitchanim "

        // Find the last space-separated token that could be a delay
        String targetText = animationData;
        double delay = DEFAULT_ANIMATION_SPEED;

        // Split and check if last part is a number (delay)
        int lastSpaceIndex = animationData.lastIndexOf(' ');
        if (lastSpaceIndex != -1) {
            String potentialDelay = animationData.substring(lastSpaceIndex + 1);
            try {
                delay = Double.parseDouble(potentialDelay);
                // If parsing succeeded, the text is everything before the delay
                targetText = animationData.substring(0, lastSpaceIndex);
            } catch (NumberFormatException e) {
                // Not a number, treat entire string as text with default delay
                targetText = animationData;
            }
        }

        if (targetText.trim().isEmpty()) {
            return new AnimationData(Arrays.asList(""), delay);
        }

        List<String> frames = generateGlitchFrames(targetText);
        return new AnimationData(frames, delay);
    }

    private static List<String> generateGlitchFrames(String targetText) {
        List<String> frames = new ArrayList<>();
        Random random = new Random();
        String glitchChars = "0123456789!@#$%^&*()";

        // Phase 1: Typewriter effect with glitch characters
        for (int i = 1; i <= targetText.length(); i++) {
            StringBuilder frame = new StringBuilder();

            // Add the correctly typed characters
            frame.append(targetText.substring(0, i - 1));

            // Add a random glitch character for the current position
            if (i <= targetText.length()) {
                frame.append(glitchChars.charAt(random.nextInt(glitchChars.length())));
            }

            frames.add(frame.toString());
        }

        // Add the final correct text
        frames.add(targetText);

        // Phase 2: Glitch sweep effect (replace each character with glitch, then back)
        for (int i = 0; i < targetText.length(); i++) {
            StringBuilder frame = new StringBuilder(targetText);

            // Replace character at position i with a glitch character
            if (i < frame.length()) {
                frame.setCharAt(i, glitchChars.charAt(random.nextInt(glitchChars.length())));
            }

            frames.add(frame.toString());
        }

        // End with the final correct text
        frames.add(targetText);

        return frames;
    }

    private static void stopAnimation(String animationType) {
        ScheduledFuture<?> existingAnimation = activeAnimations.get(animationType);
        if (existingAnimation != null && !existingAnimation.isDone()) {
            existingAnimation.cancel(true);
            activeAnimations.remove(animationType);
        }
    }

    private static void startTextAnimation(String animationType, AnimationData animData, Consumer<String> displayFunction) {
        // Stop any existing animation of RegionScript type
        stopAnimation(animationType);

        List<String> frames = animData.frames;
        double delaySeconds = animData.delay;

        if (frames.isEmpty()) return;

        // If only one frame, just display it
        if (frames.size() == 1) {
            displayFunction.accept(frames.get(0));
            return;
        }

        // Create a single-run animation task
        ScheduledFuture<?> animationTask = scheduler.schedule(() -> {
            try {
                for (int i = 0; i < frames.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    final String frame = frames.get(i);
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> displayFunction.accept(frame));

                    // Wait between frames (except for the last frame)
                    if (i < frames.size() - 1) {
                        Thread.sleep((long)(delaySeconds * 1000));
                    }
                }

                // Remove from active animations when done
                activeAnimations.remove(animationType);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                activeAnimations.remove(animationType);
            }
        }, 0, TimeUnit.MILLISECONDS);

        activeAnimations.put(animationType, animationTask);
    }



    // Command handlers
    private static void handleGoToHome(CommandContext ctx) {
        if (ctx.args.isEmpty()) {
            sendClientMessage("No home name provided after gotohome");
            return;
        }

        String name = ctx.args.get(0);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.networkHandler.sendChatCommand("home" + " " + name);
        }
    }

    private static void handlePopup(CommandContext ctx) {
        if (ctx.args.isEmpty()) {
            sendClientMessage("Usage: !popup <text>");
            return;
        }

        String popupText = String.join(" ", ctx.args);

        // Show popup on main thread
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            Screen popupScreen = new PopupScreen(popupText);
            client.setScreen(popupScreen);
        });
    }

    private static void handleShake(CommandContext ctx) {
        if (ctx.args.isEmpty()) {
            sendClientMessage("Usage: !shake <intensity> <duration (ticks)>");
            return;
        }

        float intensity = Float.parseFloat(ctx.args.get(0));
        int duration = Integer.parseInt(ctx.args.get(1));

        if (intensity <= 5.0) {
            if (duration <= 100) {
                CameraShake.executeShake(intensity, duration);
            } else {
                sendClientMessage("Camera shake duration cannot be over 100 ticks");
            }
        } else {
            sendClientMessage("Camera shake intensity cannot be over 5");
        }
    }

    private static void handlePlaySong(CommandContext ctx) {
        if (ctx.args.isEmpty()) {
            sendClientMessage("No URL provided after playsong.");
            return;
        }

        String url = ctx.args.get(0);
        sendClientMessage("Playing song from: " + url);
        new Thread(() -> playMp3FromUrl(url), "SoundSync-PlayThread").start();
    }

    private static void handleStopAll(CommandContext ctx) {
        stopAllSongs();
    }

    private static void handleShowTitle(CommandContext ctx) {
        if (ctx.args.isEmpty()) {
            sendClientMessage("Usage: !showTitle <title text> or !showTitle $anim frame1,frame2,frame3 [delay] or !showTitle $glitchanim <text> [delay]");
            return;
        }

        String titleText = String.join(" ", ctx.args);

        if (isAnimationText(titleText)) {
            AnimationData animData = parseAnimationData(titleText);
            startTextAnimation("title", animData, RegionScript::displayTitle);
        } else {
            displayTitle(titleText);
        }
    }

    private static void displayTitle(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String formattedTitle = text.replace('%', '§');
            client.inGameHud.setTitle(Text.literal(formattedTitle));
            client.inGameHud.setTitleTicks(10, 60, 10);
        }
    }

    private static void handleShowSubtitle(CommandContext ctx) {
        if (ctx.args.isEmpty()) {
            sendClientMessage("Usage: !showSubtitle <subtitle text> or !showSubtitle $anim frame1,frame2,frame3 [delay] or !showSubtitle $glitchanim <text> [delay]");
            return;
        }

        String subtitleText = String.join(" ", ctx.args);

        if (isAnimationText(subtitleText)) {
            AnimationData animData = parseAnimationData(subtitleText);
            startTextAnimation("subtitle", animData, RegionScript::displaySubtitle);
        } else {
            displaySubtitle(subtitleText);
        }
    }

    private static void displaySubtitle(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String formattedSubtitle = text.replace('%', '§');
            client.inGameHud.setSubtitle(Text.literal(formattedSubtitle));
            client.inGameHud.setTitleTicks(10, 60, 10);
        }
    }

    private static void handleShowActionBar(CommandContext ctx) {
        if (ctx.args.isEmpty()) {
            sendClientMessage("Usage: !showActionBar <action bar text> or !showActionBar $anim frame1,frame2,frame3 [delay] or !showActionBar $glitchanim <text> [delay]");
            return;
        }

        String actionBarText = String.join(" ", ctx.args);

        if (isAnimationText(actionBarText)) {
            AnimationData animData = parseAnimationData(actionBarText);
            startTextAnimation("actionbar", animData, RegionScript::displayActionBar);
        } else {
            displayActionBar(actionBarText);
        }
    }

    private static void displayActionBar(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String formattedActionBar = text.replace('%', '§');
            client.inGameHud.setOverlayMessage(Text.literal(formattedActionBar), false);
        }
    }

    private static void handleShowTimer(CommandContext ctx) {
        if (ctx.args.size() < 2) {
            sendClientMessage("Usage: !showTimer <seconds> <color>");
            return;
        }

        try {
            int seconds = Integer.parseInt(ctx.args.get(0));
            String colorCode = ctx.args.get(1);

            if (seconds <= 0) {
                sendClientMessage("Timer duration must be positive");
                return;
            }

            // Stop any existing timer and action bar animation
            stopTimer(); // Use our new stop timer method

            // Start new timer
            startTimer(seconds, colorCode);

        } catch (NumberFormatException e) {
            sendClientMessage("Invalid number format for timer duration");
        }
    }

    // New command handlers
    private static void handleKill(CommandContext ctx) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            if (player.getGameMode().isCreative()) {
                player.networkHandler.sendChatCommand("kill");
            }
        } else {
            sendClientMessage("Player not found - cannot execute kill command");
        }
    }

    private static void handleStopTimer(CommandContext ctx) {
        if (timerRunning) {
            stopTimer();
        } else {
            sendClientMessage("No timer is currently running");
        }
    }

    private static void handleToggle(CommandContext ctx) {
        if (ctx.args.size() < 2) {
            sendClientMessage("Usage: !toggle <setting> <on/off>");
            sendClientMessage("Available settings: timer_death");
            return;
        }

        String setting = ctx.args.get(0).toLowerCase();
        String state = ctx.args.get(1).toLowerCase();

        // Validate setting exists
        if (!toggleSettings.containsKey(setting)) {
            sendClientMessage("Unknown setting: " + setting);
            sendClientMessage("Available settings: timer_death");
            return;
        }

        // Parse on/off state
        boolean newValue;
        if (state.equals("on") || state.equals("true") || state.equals("1")) {
            newValue = true;
        } else if (state.equals("off") || state.equals("false") || state.equals("0")) {
            newValue = false;
        } else {
            sendClientMessage("Invalid state: " + state + ". Use 'on' or 'off'");
            return;
        }

        // Update the setting
        toggleSettings.put(setting, newValue);

        // Provide feedback
        String status = newValue ? "on" : "off";
        sendClientMessage("Toggle " + setting + " is now " + status);
    }

    private static void startTimer(int totalSeconds, String colorCode) {
        timerRunning = true;
        timerEndTime = System.currentTimeMillis() + (totalSeconds * 1000L);

        // Schedule timer updates every 100ms for smooth countdown
        timerTask = scheduler.scheduleAtFixedRate(() -> {
            if (!timerRunning) {
                return; // Stop RegionScript task if timer was cancelled
            }

            long currentTime = System.currentTimeMillis();
            long remainingMs = timerEndTime - currentTime;

            if (remainingMs <= 0) {
                // Timer finished
                timerRunning = false;
                timerTask = null;

                // Check if timer_death toggle is enabled
                if (toggleSettings.getOrDefault("timer_death", false)) {
                    // Execute kill command on main thread
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> {
                        ClientPlayerEntity player = client.player;
                        if (player != null) {
                            if (player.getGameMode().isCreative()) {
                                player.networkHandler.sendChatCommand("kill");
                            }
                        }
                    });
                }

                return;
            }

            // Calculate remaining time
            int remainingSeconds = (int) (remainingMs / 1000);
            int milliseconds = (int) (remainingMs % 1000);

            // Format: SS:MMM (seconds:milliseconds)
            String timeString = String.format("%d:%03d", remainingSeconds, milliseconds);

            // Update action bar on main thread
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.player != null && timerRunning) {
                    String timerDisplay = "§" + colorCode + timeString;
                    client.inGameHud.setOverlayMessage(Text.literal(timerDisplay), false);
                }
            });

        }, 0, 100, TimeUnit.MILLISECONDS); // Update every 100ms
    }

    private static void stopTimer() {
        timerRunning = false;
        stopAnimation("actionbar");

        // Cancel the timer task if it's running
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(true);
            timerTask = null;
        }

        // Clear the action bar
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.inGameHud.setOverlayMessage(Text.literal(""), false);
            }
        });
    }

    // Utility methods
    private static void sendClientMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(message), false);
            }
        });
    }

    // Original methods (unchanged)
    private static void playMp3FromUrl(String urlString) {
        ActiveSong song = null;
        try {
            URL url = new URL(urlString);
            BufferedInputStream inputStream = new BufferedInputStream(url.openStream());
            Player player = new Player(inputStream);
            song = new ActiveSong(player, inputStream);

            synchronized (activeSongs) {
                activeSongs.add(song);
            }

            player.play(); // blocks until finished or stream closed

            // playback finished normally -> remove from active list
            synchronized (activeSongs) {
                activeSongs.remove(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // notify player of failure on client thread
            MinecraftClient client = MinecraftClient.getInstance();

            // ensure we remove partially-added song (if it was added)
            if (song != null) {
                synchronized (activeSongs) {
                    activeSongs.remove(song);
                }
            }
        }
    }

    private static void stopAllSongs() {
        synchronized (activeSongs) {
            for (ActiveSong song : activeSongs) {
                try {
                    song.inputStream.close(); // closing stream stops JLayer
                } catch (Exception ignored) {}
            }
            activeSongs.clear();
        }
    }

    // Helper classes
    private static class CommandHandler {
        final int minArgs;
        final Consumer<CommandContext> executor;

        CommandHandler(int minArgs, Consumer<CommandContext> executor) {
            this.minArgs = minArgs;
            this.executor = executor;
        }
    }

    private static class CommandContext {
        final String command;
        final List<String> args;
        final String fullMessage;

        CommandContext(String command, List<String> args, String fullMessage) {
            this.command = command;
            this.args = args;
            this.fullMessage = fullMessage;
        }
    }

    private static class ActiveSong {
        final Player player;
        final InputStream inputStream;

        ActiveSong(Player player, InputStream inputStream) {
            this.player = player;
            this.inputStream = inputStream;
        }
    }

    private static class AnimationData {
        final List<String> frames;
        final double delay;

        AnimationData(List<String> frames, double delay) {
            this.frames = frames;
            this.delay = delay;
        }
    }
}