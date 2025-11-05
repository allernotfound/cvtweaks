package org.aller.cvtweaks.client.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.security.cert.X509Certificate;
import java.time.Duration;

public class AutoUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger("CVTweaks/AutoUpdater");
    private static final String RELEASE_API = "https://api.github.com/repos/allernotfound/cvtweaks/releases/latest";

    private static final HttpClient httpClient = createHttpClient();

    private static HttpClient createHttpClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] c, String a) {}
                        public void checkServerTrusted(X509Certificate[] c, String a) {}
                    }
            };

            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, trustAll, new java.security.SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(ssl)
                    .connectTimeout(Duration.ofSeconds(8))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
        } catch (Exception e) {
            return HttpClient.newBuilder().build();
        }
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> checkUpdate());
        LOGGER.info("AutoUpdater initialized.");
    }

    private static void checkUpdate() {
        new Thread(() -> {
            try {
                String currentVersion = FabricLoader.getInstance()
                        .getModContainer("cvtweaks").get()
                        .getMetadata().getVersion().getFriendlyString();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(RELEASE_API))
                        .GET()
                        .timeout(Duration.ofSeconds(8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String latestVersion = json.get("tag_name").getAsString().replace("v", "");

                if (latestVersion.equals(currentVersion)) return;

                // Get .jar asset download URL
                JsonArray assets = json.getAsJsonArray("assets");
                if (assets.size() == 0) return;
                String downloadUrl = assets.get(0).getAsJsonObject().get("browser_download_url").getAsString();

                Path currentModPath = FabricLoader.getInstance()
                        .getModContainer("cvtweaks").get()
                        .getOrigin().getPaths().get(0);

                Path parentFolder = currentModPath.getParent();
                Path newFile = parentFolder.resolve("cvtweaks-" + latestVersion + ".jar");

                downloadFile(downloadUrl, newFile);

                send("§bA new version of CVTweaks is available: §e" + latestVersion + " §7(You have §c" + currentVersion + "§7)");
                send("§aThe updated mod has been downloaded to:");
                send("§e" + newFile.toString());
                send("§b§lPlease restart your game to apply the update.");

            } catch (Exception e) {
                LOGGER.error("Auto update check failed", e);
            }
        }).start();
    }

    private static void downloadFile(String url, Path destination) throws IOException, InterruptedException {
        // Ensure parent folder exists
        Files.createDirectories(destination.getParent());

        LOGGER.info("Downloading: " + url);
        LOGGER.info("Destination: " + destination.toAbsolutePath());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        LOGGER.info("HTTP response: {}", response.statusCode());

        if (response.statusCode() != 200) {
            send("§cDownload failed: HTTP " + response.statusCode());
            return;
        }

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(destination, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }

        if (!Files.exists(destination)) {
            send("§cDownload failed: file not created.");
        }
    }



    private static void send(String msg) {
        MinecraftClient.getInstance().execute(() ->
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(msg)));
    }
}
