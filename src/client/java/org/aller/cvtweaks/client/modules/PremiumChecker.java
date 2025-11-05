package org.aller.cvtweaks.client.modules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.aller.cvtweaks.client.cool.GradientActionBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Checks if the current player has premium access
 */
public class PremiumChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger("CVTweaks/Premium");
    private static final String API_URL = "http://66.70.176.148:65514"; // CHANGE THIS TO YOUR SERVER
    private static final HttpClient httpClient = createHttpClient();

    private static boolean isPremium = false;

    /**
     * Create HTTP client that allows insecure HTTP connections
     */
    private static HttpClient createHttpClient() {
        try {
            // Trust all certificates (allows HTTP and self-signed HTTPS)
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Failed to create HTTP client, using default", e);
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }
    }

    /**
     * Check if the current player has premium
     */
    public static boolean isPremium() {
        return isPremium;
    }

    /**
     * Check premium status from the server
     */
    public static void checkPremium() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String uuid = client.player.getUuidAsString();

        // Run in separate thread to not block game
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "/check/" + uuid))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    isPremium = json.get("premium").getAsBoolean();

                    LOGGER.info("Premium status checked: {}", isPremium);
                } else {
                    LOGGER.warn("Failed to check premium status: HTTP {}", response.statusCode());
                    isPremium = false;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to check premium status", e);
                isPremium = false;
            }
        }).start();
    }

    private static String lastServer = null;

    /**
     * Initialize the premium checker
     */
    public static void init() {
        LOGGER.info("Initializing premium checker...");

        // Check premium status when player joins a world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            checkPremium();
        });

        String currentVersion = FabricLoader.getInstance()
                .getModContainer("cvtweaks")
                .get().getMetadata().getVersion().getFriendlyString();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.getCurrentServerEntry() != null) {
                String serverIp = client.getCurrentServerEntry().address;

                // Only run if connecting to a new server
                if (!serverIp.equals(lastServer)) {
                    lastServer = serverIp;

                    // Check for your target server
                    if (serverIp.equalsIgnoreCase("cubeville.org")) {
                        MinecraftClient.getInstance().execute(() -> {
                            if (isPremium) {
                                GradientActionBar.show("Using CVTweaks " + currentVersion + " premium");
                            } else {
                                sendHudMessage("§7Using CVTweaks " + currentVersion + " free");
                            }
                        });
                    }
                }
            }
        });

        // Reset flag on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastServer = null;
        });
    }

    /**
     * Send a message to the player's HUD
     */
    private static void sendHudMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }
}